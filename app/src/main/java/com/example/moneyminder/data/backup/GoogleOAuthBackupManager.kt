package com.example.moneyminder.data.backup

import android.util.Base64
import com.example.moneyminder.data.model.BackupMetadata
import com.example.moneyminder.data.model.BackupType
import com.example.moneyminder.data.model.MoneyMinderBackupFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gmail REST API client for backup storage authenticated via Google OAuth 2.0.
 *
 * Direct HTTPS communication with Gmail API:
 * - Uploads `.mmbackup` JSON backup files directly to the user's Gmail mailbox.
 * - Lists, restores, and deletes backups without third-party cloud servers.
 * - Automatically refreshes expired OAuth tokens in the background.
 */
class GoogleOAuthBackupManager(
    private val prefs: BackupPreferences
) {

    companion object {
        const val BACKUP_SUBJECT_PREFIX = "[MoneyMinderBackup]"
        const val GMAIL_API_BASE = "https://gmail.googleapis.com/gmail/v1/users/me"
        const val BACKUP_FILE_EXTENSION = ".mmbackup"
    }

    sealed class BackupResult {
        data class Success(val gmailMessageId: String, val sizeBytes: Long) : BackupResult()
        data class Failure(val message: String) : BackupResult()
    }

    sealed class RestoreResult {
        data class Success(val content: String) : RestoreResult()
        data class Failure(val message: String) : RestoreResult()
    }

    // ── Token Management with Auto-Refresh ────────────────────────────────────

    private suspend fun getValidAccessToken(): String? {
        val currentToken = prefs.oauthAccessToken
        if (currentToken.isNotBlank()) return currentToken

        // Attempt refresh
        val refreshRes = GoogleOAuthManager.refreshAccessToken(prefs)
        return refreshRes.getOrNull()
    }

    // ── Upload Backup ─────────────────────────────────────────────────────────

    suspend fun uploadBackup(
        backupJson: String,
        backupFile: MoneyMinderBackupFile,
        isAutomatic: Boolean
    ): BackupResult = withContext(Dispatchers.IO) {
        var token = getValidAccessToken()
            ?: return@withContext BackupResult.Failure("Not signed in. Please connect your Google account.")

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            val dateLabel = sdf.format(Date(backupFile.createdAt))
            val typeLabel = if (isAutomatic) "AUTO" else "MANUAL"
            val balLabel = "bal:${f(backupFile.bankBalanceSnapshot)}:${f(backupFile.walletBalanceSnapshot)}:${f(backupFile.cashBalanceSnapshot)}"
            val subject = "$BACKUP_SUBJECT_PREFIX $dateLabel | ${backupFile.transactionCount} tx | v${backupFile.version} | $typeLabel | bid:${backupFile.backupId} | $balLabel"

            val boundary = "===moneyminder_backup_boundary==="
            val fileName = "moneyminder_backup_${backupFile.backupId}$BACKUP_FILE_EXTENSION"
            val jsonBase64 = Base64.encodeToString(backupJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val toEmail = prefs.connectedEmail.ifBlank { "me" }

            val rawEmail = buildString {
                appendLine("From: $toEmail")
                appendLine("To: $toEmail")
                appendLine("Subject: $subject")
                appendLine("MIME-Version: 1.0")
                appendLine("Content-Type: multipart/mixed; boundary=\"$boundary\"")
                appendLine()
                appendLine("--$boundary")
                appendLine("Content-Type: text/plain; charset=UTF-8")
                appendLine()
                appendLine("Money Minder Backup")
                appendLine("Date: $dateLabel")
                appendLine("Type: $typeLabel")
                appendLine("Transactions: ${backupFile.transactionCount}")
                appendLine("Backup ID: ${backupFile.backupId}")
                appendLine("Bank Balance: ₹${f(backupFile.bankBalanceSnapshot)}")
                appendLine("Wallet Balance: ₹${f(backupFile.walletBalanceSnapshot)}")
                appendLine("Cash Balance: ₹${f(backupFile.cashBalanceSnapshot)}")
                appendLine()
                appendLine("This backup email was created by Money Minder.")
                appendLine("Do not delete this email if you want to be able to restore your data.")
                appendLine()
                appendLine("--$boundary")
                appendLine("Content-Type: application/octet-stream; name=\"$fileName\"")
                appendLine("Content-Transfer-Encoding: base64")
                appendLine("Content-Disposition: attachment; filename=\"$fileName\"")
                appendLine()
                appendLine(jsonBase64)
                appendLine("--$boundary--")
            }

            val encodedEmail = Base64.encodeToString(
                rawEmail.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val requestBody = JSONObject().put("raw", encodedEmail).toString()
            var code = sendGmailMessage(token, requestBody)

            // Auto-refresh token if 401 Unauthorized
            if (code.first == 401) {
                val refreshRes = GoogleOAuthManager.refreshAccessToken(prefs)
                val refreshedToken = refreshRes.getOrNull()
                if (refreshedToken != null) {
                    token = refreshedToken
                    code = sendGmailMessage(token, requestBody)
                }
            }

            if (code.first in 200..201) {
                val sizeBytes = backupJson.toByteArray().size.toLong()
                BackupResult.Success(backupFile.backupId, sizeBytes)
            } else {
                BackupResult.Failure("Upload failed: HTTP ${code.first} — ${code.second}")
            }
        } catch (e: Exception) {
            BackupResult.Failure("Upload error: ${e.message}")
        }
    }

    private fun sendGmailMessage(token: String, requestBody: String): Pair<Int, String> {
        val url = URL("$GMAIL_API_BASE/messages/send")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 20000
            readTimeout = 20000
        }
        OutputStreamWriter(conn.outputStream).use { it.write(requestBody) }
        val code = conn.responseCode
        val response = if (code in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            conn.errorStream?.bufferedReader()?.readText() ?: ""
        }
        return Pair(code, response)
    }

    // ── List Backups ──────────────────────────────────────────────────────────

    suspend fun listBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        var token = getValidAccessToken()
            ?: return@withContext Result.failure(Exception("Not signed in"))

        try {
            val query = "subject:$BACKUP_SUBJECT_PREFIX"
            val url = URL("$GMAIL_API_BASE/messages?q=${java.net.URLEncoder.encode(query, "UTF-8")}&maxResults=50")
            var conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (conn.responseCode == 401) {
                val refreshedToken = GoogleOAuthManager.refreshAccessToken(prefs).getOrNull()
                if (refreshedToken != null) {
                    token = refreshedToken
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Authorization", "Bearer $token")
                        connectTimeout = 15000
                        readTimeout = 15000
                    }
                }
            }

            if (conn.responseCode != 200) {
                return@withContext Result.failure(Exception("Gmail list failed: HTTP ${conn.responseCode}"))
            }

            val response = conn.inputStream.bufferedReader().readText()
            val messagesArray = JSONObject(response).optJSONArray("messages") ?: JSONArray()

            val metadataList = mutableListOf<BackupMetadata>()
            for (i in 0 until messagesArray.length()) {
                val msgId = messagesArray.getJSONObject(i).getString("id")
                val meta = fetchBackupMetadata(token, msgId)
                if (meta != null) metadataList.add(meta)
            }

            metadataList.sortByDescending { it.createdAt }
            Result.success(metadataList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchBackupMetadata(token: String, messageId: String): BackupMetadata? {
        return try {
            val url = URL("$GMAIL_API_BASE/messages/$messageId?format=metadata&metadataHeaders=Subject&metadataHeaders=Date")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode != 200) return null

            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val headers = json.optJSONObject("payload")?.optJSONArray("headers") ?: JSONArray()
            var subject = ""
            for (i in 0 until headers.length()) {
                val h = headers.getJSONObject(i)
                if (h.optString("name", "").equals("Subject", ignoreCase = true)) {
                    subject = h.optString("value", "")
                }
            }

            parseSubjectMeta(messageId, subject, json.optLong("sizeEstimate", 0L))
        } catch (e: Exception) { null }
    }

    private fun parseSubjectMeta(messageId: String, subject: String, sizeEstimate: Long): BackupMetadata? {
        if (!subject.startsWith(BACKUP_SUBJECT_PREFIX)) return null
        return try {
            val parts = subject.split("|").map { it.trim() }
            val dateStr = parts[0].removePrefix(BACKUP_SUBJECT_PREFIX).trim()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            val createdAt = try { sdf.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
            val txCount = parts.getOrNull(1)?.removeSuffix("tx")?.trim()?.toIntOrNull() ?: 0
            val typeStr = parts.getOrNull(3) ?: "MANUAL"
            val backupType = if (typeStr == "AUTO") BackupType.AUTOMATIC else BackupType.MANUAL
            val backupId = parts.firstOrNull { it.startsWith("bid:") }?.removePrefix("bid:") ?: messageId
            val balPart = parts.firstOrNull { it.startsWith("bal:") }?.removePrefix("bal:")
            val balParts = balPart?.split(":") ?: emptyList()
            val bank = balParts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val wallet = balParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val cash = balParts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

            BackupMetadata(
                gmailMessageId = messageId,
                backupId = backupId,
                createdAt = createdAt,
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

    // ── Download Backup Content ───────────────────────────────────────────────

    suspend fun downloadBackup(gmailMessageId: String): RestoreResult = withContext(Dispatchers.IO) {
        var token = getValidAccessToken()
            ?: return@withContext RestoreResult.Failure("Not signed in")

        try {
            val url = URL("$GMAIL_API_BASE/messages/$gmailMessageId?format=full")
            var conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (conn.responseCode == 401) {
                val refreshedToken = GoogleOAuthManager.refreshAccessToken(prefs).getOrNull()
                if (refreshedToken != null) {
                    token = refreshedToken
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Authorization", "Bearer $token")
                        connectTimeout = 15000
                        readTimeout = 15000
                    }
                }
            }

            if (conn.responseCode != 200) {
                return@withContext RestoreResult.Failure("Download failed: HTTP ${conn.responseCode}")
            }

            val messageJson = JSONObject(conn.inputStream.bufferedReader().readText())
            val attachmentId = findAttachmentId(messageJson)
                ?: return@withContext RestoreResult.Failure("No backup attachment (.mmbackup) found in this email")

            val attUrl = URL("$GMAIL_API_BASE/messages/$gmailMessageId/attachments/$attachmentId")
            val attConn = (attUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 15000
                readTimeout = 15000
            }
            if (attConn.responseCode != 200) return@withContext RestoreResult.Failure("Could not download attachment data")

            val attJson = JSONObject(attConn.inputStream.bufferedReader().readText())
            val encodedData = attJson.getString("data")
            val decoded = Base64.decode(encodedData.replace('-', '+').replace('_', '/'), Base64.DEFAULT)
            RestoreResult.Success(String(decoded, Charsets.UTF_8))
        } catch (e: Exception) {
            RestoreResult.Failure("Download error: ${e.message}")
        }
    }

    private fun findAttachmentId(messageJson: JSONObject): String? {
        val payload = messageJson.optJSONObject("payload") ?: return null
        val parts = payload.optJSONArray("parts") ?: return null
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            val filename = part.optString("filename", "")
            if (filename.endsWith(BACKUP_FILE_EXTENSION) || filename.contains("backup")) {
                return part.optJSONObject("body")?.optString("attachmentId")
            }
        }
        return null
    }

    // ── Delete Backup ─────────────────────────────────────────────────────────

    suspend fun deleteBackup(gmailMessageId: String): Boolean = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext false
        try {
            val url = URL("$GMAIL_API_BASE/messages/$gmailMessageId/trash")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Length", "0")
                connectTimeout = 10000
                readTimeout = 10000
            }
            conn.responseCode in 200..204
        } catch (e: Exception) { false }
    }

    private fun f(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() else "%.2f".format(d)
}
