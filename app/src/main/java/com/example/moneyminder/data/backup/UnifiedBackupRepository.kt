package com.example.moneyminder.data.backup

import com.example.moneyminder.data.model.BackupMetadata
import com.example.moneyminder.data.model.MoneyMinderBackupFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.Authenticator
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.search.SubjectTerm

/**
 * Unified Backup Manager supporting both:
 * 1. Google OAuth 2.0 (Gmail REST API)
 * 2. Direct Email SSL/TLS (Gmail, Outlook, Yahoo, Custom SMTP/IMAP)
 */
class UnifiedBackupRepository(
    private val prefs: BackupPreferences
) {
    private val oauthManager = GoogleOAuthBackupManager(prefs)

    sealed class BackupResult {
        data class Success(val messageId: String, val sizeBytes: Long) : BackupResult()
        data class Failure(val message: String) : BackupResult()
    }

    sealed class RestoreResult {
        data class Success(val content: String) : RestoreResult()
        data class Failure(val message: String) : RestoreResult()
    }

    // ── Test Direct Email Connection ──────────────────────────────────────────

    suspend fun testDirectConnection(
        email: String,
        pass: String,
        smtpHost: String,
        smtpPort: Int,
        imapHost: String,
        imapPort: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(Exception("Please enter a valid email address."))
        }
        if (cleanPass.isEmpty()) {
            return@withContext Result.failure(Exception("Please enter your email password."))
        }

        try {
            val imapProps = Properties().apply {
                put("mail.imap.host", imapHost)
                put("mail.imap.port", imapPort.toString())
                put("mail.imap.ssl.enable", "true")
                put("mail.imap.socketFactory.port", imapPort.toString())
                put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.imap.socketFactory.fallback", "false")
                put("mail.imap.connectiontimeout", "12000")
                put("mail.imap.timeout", "12000")
            }

            val session = Session.getInstance(imapProps)
            val store = session.getStore("imaps")
            store.connect(imapHost, imapPort, cleanEmail, cleanPass)
            store.close()

            Result.success(Unit)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Authentication failed. Please check your email and password."
            Result.failure(Exception(errorMsg))
        }
    }

    // ── Upload Backup ─────────────────────────────────────────────────────────

    suspend fun uploadBackup(
        backupJson: String,
        backupFile: MoneyMinderBackupFile,
        isAutomatic: Boolean
    ): BackupResult = withContext(Dispatchers.IO) {
        // If OAuth token is available, use Google OAuth REST API
        if (prefs.oauthAccessToken.isNotBlank() || prefs.refreshToken.isNotBlank()) {
            val res = oauthManager.uploadBackup(backupJson, backupFile, isAutomatic)
            return@withContext when (res) {
                is GoogleOAuthBackupManager.BackupResult.Success -> BackupResult.Success(res.gmailMessageId, res.sizeBytes)
                is GoogleOAuthBackupManager.BackupResult.Failure -> BackupResult.Failure(res.message)
            }
        }

        // Otherwise use direct SMTP
        val email = prefs.connectedEmail.trim()
        val pass = prefs.appPassword.trim()
        if (email.isEmpty() || pass.isEmpty()) {
            return@withContext BackupResult.Failure("Email account not connected. Please connect in Settings.")
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            val dateLabel = sdf.format(Date(backupFile.createdAt))
            val typeLabel = if (isAutomatic) "AUTO" else "MANUAL"
            val balLabel = "bal:${f(backupFile.bankBalanceSnapshot)}:${f(backupFile.walletBalanceSnapshot)}:${f(backupFile.cashBalanceSnapshot)}"
            val subject = "[MoneyMinderBackup] $dateLabel | ${backupFile.transactionCount} tx | v${backupFile.version} | $typeLabel | bid:${backupFile.backupId} | $balLabel"

            val smtpHost = prefs.smtpHost.ifBlank { "smtp.gmail.com" }
            val smtpPort = if (prefs.smtpPort > 0) prefs.smtpPort else 465

            val props = Properties().apply {
                put("mail.smtp.host", smtpHost)
                put("mail.smtp.port", smtpPort.toString())
                put("mail.smtp.auth", "true")
                if (smtpPort == 465) {
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.socketFactory.port", smtpPort.toString())
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.socketFactory.fallback", "false")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                }
                put("mail.smtp.connectiontimeout", "15000")
                put("mail.smtp.timeout", "15000")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(email, pass)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(email, "Money Minder"))
                setRecipient(Message.RecipientType.TO, InternetAddress(email))
                setSubject(subject, "UTF-8")
                sentDate = Date(backupFile.createdAt)

                val multipart = MimeMultipart()

                val textBody = "Money Minder Backup\nDate: $dateLabel\nTransactions: ${backupFile.transactionCount}\nBackup ID: ${backupFile.backupId}"
                val textPart = MimeBodyPart().apply { setText(textBody, "UTF-8") }
                multipart.addBodyPart(textPart)

                val fileName = "moneyminder_backup_${backupFile.backupId}.mmbackup"
                val jsonBytes = backupJson.toByteArray(Charsets.UTF_8)
                val attachmentPart = MimeBodyPart().apply {
                    dataHandler = DataHandler(ByteArrayDataSource(jsonBytes, "application/json", fileName))
                    this.fileName = fileName
                }
                multipart.addBodyPart(attachmentPart)

                setContent(multipart)
            }

            Transport.send(message)
            val sizeBytes = backupJson.toByteArray().size.toLong()
            BackupResult.Success(backupFile.backupId, sizeBytes)
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Upload error")
        }
    }

    // ── List Backups ──────────────────────────────────────────────────────────

    suspend fun listBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        if (prefs.oauthAccessToken.isNotBlank() || prefs.refreshToken.isNotBlank()) {
            return@withContext oauthManager.listBackups()
        }

        val email = prefs.connectedEmail.trim()
        val pass = prefs.appPassword.trim()
        if (email.isEmpty() || pass.isEmpty()) return@withContext Result.failure(Exception("Not connected"))

        val imapHost = prefs.imapHost.ifBlank { "imap.gmail.com" }
        val imapPort = if (prefs.imapPort > 0) prefs.imapPort else 993

        var store: javax.mail.Store? = null
        try {
            val imapProps = Properties().apply {
                put("mail.imap.host", imapHost)
                put("mail.imap.port", imapPort.toString())
                put("mail.imap.ssl.enable", "true")
                put("mail.imap.socketFactory.port", imapPort.toString())
                put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.imap.connectiontimeout", "15000")
                put("mail.imap.timeout", "15000")
            }

            val session = Session.getInstance(imapProps)
            store = session.getStore("imaps")
            store.connect(imapHost, imapPort, email, pass)

            val folder = store.getFolder("INBOX")
            folder.open(Folder.READ_ONLY)

            val searchSubject = SubjectTerm("[MoneyMinderBackup]")
            val messages = folder.search(searchSubject)

            val metadataList = mutableListOf<BackupMetadata>()
            for (msg in messages) {
                val subject = msg.subject ?: ""
                val msgNum = msg.messageNumber.toString()
                val meta = parseSubjectMeta(msgNum, subject, msg.size.toLong(), msg.sentDate?.time ?: 0L)
                if (meta != null) metadataList.add(meta)
            }

            folder.close(false)
            store.close()

            metadataList.sortByDescending { it.createdAt }
            Result.success(metadataList)
        } catch (e: Exception) {
            try { store?.close() } catch (ignored: Exception) {}
            Result.failure(e)
        }
    }

    // ── Download Backup ───────────────────────────────────────────────────────

    suspend fun downloadBackup(messageId: String): RestoreResult = withContext(Dispatchers.IO) {
        if (prefs.oauthAccessToken.isNotBlank() || prefs.refreshToken.isNotBlank()) {
            val res = oauthManager.downloadBackup(messageId)
            return@withContext when (res) {
                is GoogleOAuthBackupManager.RestoreResult.Success -> RestoreResult.Success(res.content)
                is GoogleOAuthBackupManager.RestoreResult.Failure -> RestoreResult.Failure(res.message)
            }
        }

        val email = prefs.connectedEmail.trim()
        val pass = prefs.appPassword.trim()
        if (email.isEmpty() || pass.isEmpty()) return@withContext RestoreResult.Failure("Not connected")

        val imapHost = prefs.imapHost.ifBlank { "imap.gmail.com" }
        val imapPort = if (prefs.imapPort > 0) prefs.imapPort else 993

        var store: javax.mail.Store? = null
        try {
            val imapProps = Properties().apply {
                put("mail.imap.host", imapHost)
                put("mail.imap.port", imapPort.toString())
                put("mail.imap.ssl.enable", "true")
                put("mail.imap.socketFactory.port", imapPort.toString())
                put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.imap.connectiontimeout", "15000")
                put("mail.imap.timeout", "15000")
            }

            val session = Session.getInstance(imapProps)
            store = session.getStore("imaps")
            store.connect(imapHost, imapPort, email, pass)

            val folder = store.getFolder("INBOX")
            folder.open(Folder.READ_ONLY)

            val searchSubject = SubjectTerm("[MoneyMinderBackup]")
            val messages = folder.search(searchSubject)

            var foundContent: String? = null
            for (msg in messages) {
                val subject = msg.subject ?: ""
                val num = msg.messageNumber.toString()
                if (subject.contains(messageId) || num == messageId) {
                    val content = msg.content
                    if (content is Multipart) {
                        for (i in 0 until content.count) {
                            val bodyPart = content.getBodyPart(i)
                            val fileName = bodyPart.fileName ?: ""
                            if (fileName.endsWith(".mmbackup") || Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true)) {
                                val stream = bodyPart.inputStream
                                foundContent = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                                break
                            }
                        }
                    }
                    if (foundContent != null) break
                }
            }

            folder.close(false)
            store.close()

            if (foundContent != null) {
                RestoreResult.Success(foundContent)
            } else {
                RestoreResult.Failure("Backup attachment (.mmbackup) not found in email.")
            }
        } catch (e: Exception) {
            try { store?.close() } catch (ignored: Exception) {}
            RestoreResult.Failure(e.message ?: "Download error")
        }
    }

    // ── Delete Backup ─────────────────────────────────────────────────────────

    suspend fun deleteBackup(messageId: String): Boolean = withContext(Dispatchers.IO) {
        if (prefs.oauthAccessToken.isNotBlank() || prefs.refreshToken.isNotBlank()) {
            return@withContext oauthManager.deleteBackup(messageId)
        }

        val email = prefs.connectedEmail.trim()
        val pass = prefs.appPassword.trim()
        if (email.isEmpty() || pass.isEmpty()) return@withContext false

        val imapHost = prefs.imapHost.ifBlank { "imap.gmail.com" }
        val imapPort = if (prefs.imapPort > 0) prefs.imapPort else 993

        var store: javax.mail.Store? = null
        try {
            val imapProps = Properties().apply {
                put("mail.imap.host", imapHost)
                put("mail.imap.port", imapPort.toString())
                put("mail.imap.ssl.enable", "true")
                put("mail.imap.socketFactory.port", imapPort.toString())
                put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.imap.connectiontimeout", "15000")
                put("mail.imap.timeout", "15000")
            }

            val session = Session.getInstance(imapProps)
            store = session.getStore("imaps")
            store.connect(imapHost, imapPort, email, pass)

            val folder = store.getFolder("INBOX")
            folder.open(Folder.READ_WRITE)

            val searchSubject = SubjectTerm("[MoneyMinderBackup]")
            val messages = folder.search(searchSubject)

            for (msg in messages) {
                val subject = msg.subject ?: ""
                val num = msg.messageNumber.toString()
                if (subject.contains(messageId) || num == messageId) {
                    msg.setFlag(Flags.Flag.DELETED, true)
                }
            }

            folder.close(true)
            store.close()
            true
        } catch (e: Exception) {
            try { store?.close() } catch (ignored: Exception) {}
            false
        }
    }

    private fun parseSubjectMeta(messageId: String, subject: String, sizeEstimate: Long, fallbackDate: Long): BackupMetadata? {
        if (!subject.startsWith("[MoneyMinderBackup]")) return null
        return try {
            val parts = subject.split("|").map { it.trim() }
            val dateStr = parts[0].removePrefix("[MoneyMinderBackup]").trim()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            val parsedDate = try { sdf.parse(dateStr)?.time ?: fallbackDate } catch (e: Exception) { fallbackDate }
            val txCount = parts.getOrNull(1)?.removeSuffix("tx")?.trim()?.toIntOrNull() ?: 0
            val typeStr = parts.getOrNull(3) ?: "MANUAL"
            val backupType = if (typeStr == "AUTO") com.example.moneyminder.data.model.BackupType.AUTOMATIC else com.example.moneyminder.data.model.BackupType.MANUAL
            val backupId = parts.firstOrNull { it.startsWith("bid:") }?.removePrefix("bid:") ?: messageId
            val balPart = parts.firstOrNull { it.startsWith("bal:") }?.removePrefix("bal:")
            val balParts = balPart?.split(":") ?: emptyList()
            val bank = balParts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val wallet = balParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val cash = balParts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

            BackupMetadata(
                gmailMessageId = backupId,
                backupId = backupId,
                createdAt = if (parsedDate > 0) parsedDate else System.currentTimeMillis(),
                transactionCount = txCount,
                sizeBytes = sizeEstimate,
                type = backupType,
                version = 1,
                bankSnapshot = bank,
                walletSnapshot = wallet,
                cashSnapshot = cash
            )
        } catch (e: Exception) { null }
    }

    private fun f(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() else "%.2f".format(d)

    private class ByteArrayDataSource(
        private val data: ByteArray,
        private val type: String,
        private val name: String
    ) : DataSource {
        override fun getInputStream(): InputStream = ByteArrayInputStream(data)
        override fun getOutputStream(): OutputStream = throw UnsupportedOperationException("Read-only")
        override fun getContentType(): String = type
        override fun getName(): String = name
    }
}
