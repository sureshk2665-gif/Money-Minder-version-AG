package com.example.moneyminder.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyminder.data.backup.BackupPreferences
import com.example.moneyminder.data.backup.BackupSerializer
import com.example.moneyminder.data.backup.GoogleOAuthManager
import com.example.moneyminder.data.backup.ScheduledBackupWorker
import com.example.moneyminder.data.backup.UnifiedBackupRepository
import com.example.moneyminder.data.db.TransactionDao
import com.example.moneyminder.data.model.BackupFrequency
import com.example.moneyminder.data.model.BackupMetadata
import com.example.moneyminder.data.model.BackupOperationStatus
import com.example.moneyminder.data.model.BackupRestoreMode
import com.example.moneyminder.data.model.BackupSettingsRecord
import com.example.moneyminder.data.model.BackupStatus
import com.example.moneyminder.data.model.CategoryEntity
import com.example.moneyminder.data.model.MergePreview
import com.example.moneyminder.data.model.MoneyMinderBackupFile
import com.example.moneyminder.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    val prefs = BackupPreferences(context)
    private val dao = TransactionDao(context)
    private val repository = UnifiedBackupRepository(prefs)

    // ── UI State ──────────────────────────────────────────────────────────────

    private val _backupStatus = MutableStateFlow(buildStatusFromPrefs())
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _backupHistory = MutableStateFlow<List<BackupMetadata>>(emptyList())
    val backupHistory: StateFlow<List<BackupMetadata>> = _backupHistory.asStateFlow()

    private val _selectedBackupForRestore = MutableStateFlow<BackupMetadata?>(null)
    val selectedBackupForRestore: StateFlow<BackupMetadata?> = _selectedBackupForRestore.asStateFlow()

    private val _mergePreview = MutableStateFlow<MergePreview?>(null)
    val mergePreview: StateFlow<MergePreview?> = _mergePreview.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Notifies MainViewModel to reload all data after a restore
    private val _restoreComplete = MutableStateFlow(false)
    val restoreComplete: StateFlow<Boolean> = _restoreComplete.asStateFlow()

    // ── Browser OAuth 2.0 Flow ────────────────────────────────────────────────

    fun setCustomClientId(clientId: String) {
        prefs.customClientId = clientId.trim()
    }

    fun startOAuthInBrowser(ctx: Context) {
        try {
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Opening browser for Google sign-in…", 0.1f)
            GoogleOAuthManager.startBrowserSignIn(
                context = ctx,
                prefs = prefs,
                scope = viewModelScope,
                onCodeReceived = { code ->
                    completeOAuthWithCode(code)
                },
                onError = { err ->
                    setOperationStatus(BackupOperationStatus.FAILED, err)
                    toast(err)
                }
            )
            toast("Please complete sign-in in your browser")
        } catch (e: Exception) {
            setOperationStatus(BackupOperationStatus.FAILED, "Could not open browser: ${e.message}")
            toast("Could not open browser. Please try again.")
        }
    }

    fun handleOAuthCallback(uri: Uri) {
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            setOperationStatus(BackupOperationStatus.FAILED, "Sign-in error: $error")
            toast("Sign-in cancelled or denied")
            return
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            setOperationStatus(BackupOperationStatus.FAILED, "No authorization code received")
            return
        }

        completeOAuthWithCode(code)
    }

    private fun completeOAuthWithCode(code: String) {
        viewModelScope.launch {
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Connecting account…", 0.5f)
            val result = GoogleOAuthManager.exchangeCodeForTokens(code, prefs)
            result.fold(
                onSuccess = { response ->
                    prefs.oauthAccessToken = response.accessToken
                    prefs.refreshToken = response.refreshToken
                    prefs.connectedEmail = response.email
                    prefs.connectedName = response.name
                    prefs.isConnected = true

                    _backupStatus.update { buildStatusFromPrefs() }
                    setOperationStatus(BackupOperationStatus.SUCCESS, "Connected to ${response.email}", 1f)
                    toast("Connected to ${response.email} successfully!")
                },
                onFailure = { err ->
                    setOperationStatus(BackupOperationStatus.FAILED, "Connection failed: ${err.message}")
                    toast("Connection failed. Please try again.")
                }
            )
        }
    }

    // ── Direct Email Connection (Email + Password) ────────────────────────────

    fun connectDirectEmail(
        email: String,
        password: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            val msg = "Please enter a valid email address."
            setOperationStatus(BackupOperationStatus.FAILED, msg)
            toast(msg)
            onError(msg)
            return
        }

        if (password.isBlank()) {
            // Instant connect: save email for 1-Tap Gmail backups
            prefs.connectedEmail = cleanEmail
            prefs.appPassword = ""
            prefs.isConnected = true
            _backupStatus.update { buildStatusFromPrefs() }
            setOperationStatus(BackupOperationStatus.SUCCESS, "Email saved: $cleanEmail ✓")
            toast("Email saved: $cleanEmail")
            onSuccess()
            return
        }

        // Direct SMTP/IMAP verification if password is provided
        viewModelScope.launch {
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Verifying credentials…", 0.3f)
            val isGmail = cleanEmail.endsWith("@gmail.com", ignoreCase = true)
            val smtpHost = if (isGmail) "smtp.gmail.com" else prefs.smtpHost
            val imapHost = if (isGmail) "imap.gmail.com" else prefs.imapHost

            val result = repository.testDirectConnection(
                email = cleanEmail,
                pass = password.trim(),
                smtpHost = smtpHost,
                smtpPort = 465,
                imapHost = imapHost,
                imapPort = 993
            )
            result.fold(
                onSuccess = {
                    prefs.connectedEmail = cleanEmail
                    prefs.appPassword = password.trim()
                    prefs.isConnected = true
                    _backupStatus.update { buildStatusFromPrefs() }
                    setOperationStatus(BackupOperationStatus.SUCCESS, "Connected to $cleanEmail ✓")
                    toast("Connected to $cleanEmail!")
                    onSuccess()
                },
                onFailure = { error ->
                    val rawMsg = error.message ?: "Authentication failed"
                    val userFriendlyMsg = if (rawMsg.contains("Application-specific password required") || rawMsg.contains("185833")) {
                        "Gmail requires a 16-letter App Password (myaccount.google.com/apppasswords). Or leave password blank to use 1-Tap Gmail Backup!"
                    } else {
                        rawMsg
                    }
                    setOperationStatus(BackupOperationStatus.FAILED, userFriendlyMsg)
                    toast(userFriendlyMsg)
                    onError(userFriendlyMsg)
                }
            )
        }
    }


    // ── Disconnect account ────────────────────────────────────────────────────

    fun disconnectAccount() {
        prefs.oauthAccessToken = ""
        prefs.refreshToken = ""
        prefs.appPassword = ""
        prefs.oauthCodeVerifier = ""
        prefs.connectedEmail = ""
        prefs.connectedName = ""
        prefs.isConnected = false
        prefs.isAutoBackupEnabled = false

        ScheduledBackupWorker.cancel(context)
        _backupStatus.update { buildStatusFromPrefs() }
        _backupHistory.value = emptyList()
        toast("Account disconnected. Your local data is unchanged.")
    }

    // ── Auto backup toggle ────────────────────────────────────────────────────

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.isAutoBackupEnabled = enabled
        _backupStatus.update { it.copy(isAutoEnabled = enabled) }
        if (enabled) {
            ScheduledBackupWorker.schedule(context, prefs.backupFrequency)
        } else {
            ScheduledBackupWorker.cancel(context)
        }
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        prefs.backupFrequency = frequency
        _backupStatus.update { it.copy(frequency = frequency) }
        if (prefs.isAutoBackupEnabled) {
            ScheduledBackupWorker.schedule(context, frequency)
        }
    }

    // ── Backup Now ────────────────────────────────────────────────────────────

    /**
     * Share backup directly via Android's native email / Gmail app.
     * Guaranteed to work 100% with zero authentication errors or cloud setup.
     */
    fun shareBackupViaEmail(ctx: Context) {
        viewModelScope.launch {
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Generating backup…", 0.3f)
            try {
                val transactions = withContext(Dispatchers.IO) { dao.getAllCalculatedTransactions() }
                val categories = withContext(Dispatchers.IO) { dao.getAllCategories() }
                val balances = withContext(Dispatchers.IO) { dao.getCurrentBalances() }
                val backupId = UUID.randomUUID().toString().replace("-", "").take(12)

                val backupFile = BackupSerializer.serialize(
                    transactions = transactions,
                    categories = categories,
                    settings = BackupSettingsRecord(
                        autoBackupEnabled = prefs.isAutoBackupEnabled,
                        backupFrequency = prefs.backupFrequency.name,
                        connectedEmail = prefs.connectedEmail
                    ),
                    bankBalance = balances.bankBalance,
                    walletBalance = balances.walletBalance,
                    cashBalance = balances.cashBalance,
                    backupId = backupId
                )

                val backupJson = BackupSerializer.toJson(backupFile)
                val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                val fileName = "MoneyMinder_Backup_${sdf.format(java.util.Date())}.mmbackup"

                val cacheFile = java.io.File(ctx.cacheDir, fileName)
                cacheFile.writeText(backupJson, Charsets.UTF_8)

                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    ctx,
                    "${ctx.packageName}.fileprovider",
                    cacheFile
                )

                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    if (prefs.connectedEmail.isNotBlank()) {
                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(prefs.connectedEmail))
                    }
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "[MoneyMinderBackup] ${java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())} | ${transactions.size} tx | bid:$backupId")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Here is your private Money Minder backup file.\n\nTransactions: ${transactions.size}\nBackup ID: $backupId\nDate: ${java.util.Date()}\n\nKeep this file safe to restore your data anytime in Money Minder.")
                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = android.content.Intent.createChooser(sendIntent, "Send Backup to Email").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(chooser)

                prefs.lastBackupAt = System.currentTimeMillis()
                prefs.lastBackupId = backupId
                prefs.lastBackupSizeBytes = backupJson.toByteArray().size.toLong()
                _backupStatus.update { buildStatusFromPrefs().copy(
                    operationStatus = BackupOperationStatus.SUCCESS,
                    operationMessage = "Backup ready to send ✓",
                    operationProgress = 1f
                )}
                toast("Backup created — choose Gmail to send to your inbox")
            } catch (e: Exception) {
                setOperationStatus(BackupOperationStatus.FAILED, "Error: ${e.message}")
                toast("Could not create backup: ${e.message}")
            }
        }
    }

    fun restoreFromFileUri(ctx: Context, uri: Uri, mode: BackupRestoreMode) {
        viewModelScope.launch {
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Reading backup file…", 0.2f)
            try {
                val json = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                } ?: throw Exception("Could not read backup file")

                val parseResult = BackupSerializer.fromJson(json)
                if (parseResult.file == null) {
                    setOperationStatus(BackupOperationStatus.FAILED, parseResult.error ?: "Invalid backup file")
                    toast(parseResult.error ?: "Invalid backup file")
                    return@launch
                }

                setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Restoring data…", 0.6f)
                val ok = withContext(Dispatchers.IO) {
                    applyRestore(parseResult.file, mode)
                }

                if (ok) {
                    setOperationStatus(BackupOperationStatus.SUCCESS, "Restore complete ✓", 1f)
                    toast("Restored ${parseResult.file.transactionCount} transactions successfully!")
                    _restoreComplete.value = true
                } else {
                    setOperationStatus(BackupOperationStatus.FAILED, "Restore failed")
                    toast("Restore failed. Your local data is unchanged.")
                }
            } catch (e: Exception) {
                setOperationStatus(BackupOperationStatus.FAILED, "Error: ${e.message}")
                toast("Error restoring file: ${e.message}")
            }
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            if (!prefs.isConnected) {
                toast("Connect your email account first")
                return@launch
            }
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Preparing backup…", 0.1f)

            val result = withContext(Dispatchers.IO) {
                try {
                    val transactions = dao.getAllCalculatedTransactions()
                    val categories = dao.getAllCategories()
                    val balances = dao.getCurrentBalances()
                    val backupId = UUID.randomUUID().toString().replace("-", "").take(12)

                    setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Serializing data…", 0.35f)
                    val backupFile = BackupSerializer.serialize(
                        transactions = transactions,
                        categories = categories,
                        settings = BackupSettingsRecord(
                            autoBackupEnabled = prefs.isAutoBackupEnabled,
                            backupFrequency = prefs.backupFrequency.name,
                            connectedEmail = prefs.connectedEmail
                        ),
                        bankBalance = balances.bankBalance,
                        walletBalance = balances.walletBalance,
                        cashBalance = balances.cashBalance,
                        backupId = backupId
                    )

                    val backupJson = BackupSerializer.toJson(backupFile)
                    setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Uploading backup…", 0.65f)

                    val uploadResult = repository.uploadBackup(backupJson, backupFile, isAutomatic = false)
                    Pair(uploadResult, backupFile)
                } catch (e: Exception) {
                    Pair(UnifiedBackupRepository.BackupResult.Failure("Error: ${e.message}"), null)
                }
            }

            val (uploadResult, backupFile) = result
            when (uploadResult) {
                is UnifiedBackupRepository.BackupResult.Success -> {
                    prefs.lastBackupAt = System.currentTimeMillis()
                    prefs.lastBackupId = backupFile?.backupId ?: ""
                    prefs.lastBackupSizeBytes = uploadResult.sizeBytes
                    prefs.lastBackupStatus = "Manual backup successful"
                    prefs.lastSuccessfulBackupHash = BackupSerializer.computeDataHash(
                        withContext(Dispatchers.IO) { dao.getAllCalculatedTransactions() }
                    )
                    _backupStatus.update { buildStatusFromPrefs().copy(
                        operationStatus = BackupOperationStatus.SUCCESS,
                        operationMessage = "Backup complete ✓  ${uploadResult.sizeBytes / 1024} KB",
                        operationProgress = 1f
                    )}
                    toast("Backup successful — ${uploadResult.sizeBytes / 1024} KB")
                }
                is UnifiedBackupRepository.BackupResult.Failure -> {
                    setOperationStatus(BackupOperationStatus.FAILED, "Backup failed: ${uploadResult.message}")
                    toast("Backup failed. Your local data is unchanged.")
                }
            }
        }
    }


    // ── Load backup history ───────────────────────────────────────────────────

    fun loadBackupHistory() {
        viewModelScope.launch {
            if (!prefs.isConnected) return@launch
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Loading backup history…")
            val result = withContext(Dispatchers.IO) { repository.listBackups() }
            result.fold(
                onSuccess = {
                    _backupHistory.value = it
                    setOperationStatus(BackupOperationStatus.IDLE, "")
                },
                onFailure = {
                    setOperationStatus(BackupOperationStatus.FAILED, "Could not load backups: ${it.message}")
                }
            )
        }
    }

    // ── Delete a backup ───────────────────────────────────────────────────────

    fun deleteBackup(meta: BackupMetadata) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) { repository.deleteBackup(meta.gmailMessageId) }
            if (deleted) {
                _backupHistory.update { it.filter { b -> b.gmailMessageId != meta.gmailMessageId } }
                toast("Backup deleted")
            } else {
                toast("Could not delete backup. Please try again.")
            }
        }
    }

    // ── Restore flow ──────────────────────────────────────────────────────────

    fun selectBackupForRestore(meta: BackupMetadata) {
        _selectedBackupForRestore.value = meta
        _mergePreview.value = null
    }

    /** Load the selected backup's content and build a merge preview (without committing). */
    fun previewMerge(meta: BackupMetadata) {
        viewModelScope.launch {
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Analysing backup…")
            val downloadResult = withContext(Dispatchers.IO) { repository.downloadBackup(meta.gmailMessageId) }
            when (downloadResult) {
                is UnifiedBackupRepository.RestoreResult.Success -> {
                    val parseResult = BackupSerializer.fromJson(downloadResult.content)
                    if (parseResult.file == null) {
                        setOperationStatus(BackupOperationStatus.FAILED, parseResult.error ?: "Parse error")
                        return@launch
                    }
                    val backupFile = parseResult.file
                    val existing = withContext(Dispatchers.IO) { dao.getAllCalculatedTransactions() }
                    val existingIds = existing.map { it.id }.toSet()

                    val incomingTx = backupFile.transactions.mapNotNull { BackupSerializer.toTransactionEntity(it) }
                    val newTxCount = incomingTx.count { it.id !in existingIds }
                    val dupCount = incomingTx.count { it.id in existingIds }
                    val transferCount = incomingTx.count { it.type.name == "TRANSFER" }

                    val existingCatNames = withContext(Dispatchers.IO) { dao.getAllCategories() }.map { it.name }.toSet()
                    val newCatCount = backupFile.categories.count { it.name !in existingCatNames }

                    val preview = MergePreview(
                        newTransactionsToAdd = newTxCount,
                        existingTransactionsKept = existing.size,
                        possibleDuplicates = dupCount,
                        transfersDetected = transferCount,
                        categoriesToAdd = newCatCount,
                        bankBalanceAfter = backupFile.bankBalanceSnapshot,
                        walletBalanceAfter = backupFile.walletBalanceSnapshot,
                        cashBalanceAfter = backupFile.cashBalanceSnapshot
                    )
                    _mergePreview.value = preview
                    setOperationStatus(BackupOperationStatus.IDLE, "")
                }
                is UnifiedBackupRepository.RestoreResult.Failure -> {
                    setOperationStatus(BackupOperationStatus.FAILED, "Could not load backup: ${downloadResult.message}")
                }
            }
        }
    }

    /** Perform the actual restore. Called after user confirms. */
    fun performRestore(meta: BackupMetadata, mode: BackupRestoreMode) {
        viewModelScope.launch {
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Downloading backup…", 0.1f)
            val downloadResult = withContext(Dispatchers.IO) { repository.downloadBackup(meta.gmailMessageId) }
            when (downloadResult) {
                is UnifiedBackupRepository.RestoreResult.Success -> {
                    val parseResult = BackupSerializer.fromJson(downloadResult.content)
                    if (parseResult.file == null) {
                        setOperationStatus(BackupOperationStatus.FAILED, parseResult.error ?: "Corrupt backup file")
                        return@launch
                    }
                    setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Restoring data…", 0.5f)
                    val restoreOk = withContext(Dispatchers.IO) {
                        applyRestore(parseResult.file, mode)
                    }
                    if (restoreOk) {
                        setOperationStatus(BackupOperationStatus.SUCCESS, "Restore complete ✓", 1f)
                        toast("Restore complete — data reloaded")
                        _restoreComplete.value = true
                    } else {
                        setOperationStatus(BackupOperationStatus.FAILED, "Restore failed — previous data preserved")
                        toast("Restore failed. Your existing data is unchanged.")
                    }
                }
                is UnifiedBackupRepository.RestoreResult.Failure -> {
                    setOperationStatus(BackupOperationStatus.FAILED, "Download failed: ${downloadResult.message}")
                }
            }
        }
    }

    private fun applyRestore(file: MoneyMinderBackupFile, mode: BackupRestoreMode): Boolean {
        return try {
            when (mode) {
                BackupRestoreMode.REPLACE -> {
                    dao.deleteAllData()
                    val entities = file.transactions.mapNotNull { BackupSerializer.toTransactionEntity(it) }
                    val categories = file.categories.map { BackupSerializer.toCategoryEntity(it) }
                    dao.insertAllForRestore(entities)
                    dao.insertCategoriesForRestore(categories)
                }
                BackupRestoreMode.MERGE -> {
                    val existing = dao.getAllCalculatedTransactions()
                    val existingIds = existing.map { it.id }.toSet()
                    val toInsert = file.transactions
                        .mapNotNull { BackupSerializer.toTransactionEntity(it) }
                        .filter { it.id !in existingIds }
                    dao.insertAllForRestore(toInsert)

                    val existingCatNames = dao.getAllCategories().map { it.name }.toSet()
                    val newCats = file.categories
                        .map { BackupSerializer.toCategoryEntity(it) }
                        .filter { it.name !in existingCatNames }
                    dao.insertCategoriesForRestore(newCats)
                }
            }
            true
        } catch (e: Exception) { false }
    }

    fun acknowledgeRestore() { _restoreComplete.value = false }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun clearToast() { _toastMessage.value = null }
    fun clearOperationStatus() { _backupStatus.update { buildStatusFromPrefs() } }

    private fun toast(msg: String) { _toastMessage.value = msg }

    private fun setOperationStatus(status: BackupOperationStatus, message: String, progress: Float = 0f) {
        _backupStatus.update { it.copy(operationStatus = status, operationMessage = message, operationProgress = progress) }
    }

    private fun buildStatusFromPrefs() = BackupStatus(
        isConnected = prefs.isConnected,
        connectedEmail = prefs.connectedEmail,
        lastBackupAt = prefs.lastBackupAt,
        lastBackupId = prefs.lastBackupId,
        lastBackupSizeBytes = prefs.lastBackupSizeBytes,
        lastBackupStatus = prefs.lastBackupStatus,
        isAutoEnabled = prefs.isAutoBackupEnabled,
        frequency = prefs.backupFrequency,
        operationStatus = BackupOperationStatus.IDLE
    )
}
