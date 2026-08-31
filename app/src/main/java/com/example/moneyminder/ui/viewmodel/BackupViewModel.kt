package com.example.moneyminder.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyminder.data.backup.BackupFileManager
import com.example.moneyminder.data.backup.BackupPreferences
import com.example.moneyminder.data.backup.BackupSerializer
import com.example.moneyminder.data.backup.ScheduledBackupWorker
import com.example.moneyminder.data.db.TransactionDao
import com.example.moneyminder.data.model.BackupOperationStatus
import com.example.moneyminder.data.model.BackupSettingsRecord
import com.example.moneyminder.data.model.BackupStatus
import com.example.moneyminder.data.model.MoneyMinderBackupFile
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
    private val prefs = BackupPreferences(context)
    private val dao = TransactionDao(context)

    private val _backupStatus = MutableStateFlow(buildStatusFromPrefs())
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _restoreComplete = MutableStateFlow(false)
    val restoreComplete: StateFlow<Boolean> = _restoreComplete.asStateFlow()

    private val _needsStoragePermission = MutableStateFlow(false)
    val needsStoragePermission: StateFlow<Boolean> = _needsStoragePermission.asStateFlow()

    init {
        ScheduledBackupWorker.scheduleAll(context)
        checkStoragePermission()
    }

    fun checkStoragePermission() {
        _needsStoragePermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !Environment.isExternalStorageManager()
        } else {
            false
        }
    }

    fun hasStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            if (!hasStorageAccess()) {
                toast("Grant 'All Files Access' permission first")
                _needsStoragePermission.value = true
                return@launch
            }
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Creating backup...", 0.2f)
            try {
                val transactions = withContext(Dispatchers.IO) { dao.getAllCalculatedTransactions() }
                val categories = withContext(Dispatchers.IO) { dao.getAllCategories() }
                val balances = withContext(Dispatchers.IO) { dao.getCurrentBalances() }
                val backupId = UUID.randomUUID().toString().replace("-", "").take(12)

                setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Saving backup...", 0.6f)

                val backupFile = BackupSerializer.serialize(
                    transactions = transactions,
                    categories = categories,
                    settings = BackupSettingsRecord(autoBackupEnabled = true),
                    bankBalance = balances.bankBalance,
                    walletBalance = balances.walletBalance,
                    cashBalance = balances.cashBalance,
                    backupId = backupId
                )

                val backupJson = BackupSerializer.toJson(backupFile)
                val file = BackupFileManager.getManualBackupFile(context)
                withContext(Dispatchers.IO) {
                    file.parentFile?.mkdirs()
                    file.writeText(backupJson, Charsets.UTF_8)
                }

                prefs.lastBackupAt = System.currentTimeMillis()
                prefs.lastBackupSizeBytes = file.length()
                prefs.lastBackupStatus = "Manual backup successful"
                prefs.lastSuccessfulBackupHash = BackupSerializer.computeDataHash(transactions)

                _backupStatus.update {
                    buildStatusFromPrefs().copy(
                        operationStatus = BackupOperationStatus.SUCCESS,
                        operationMessage = "Backup saved: ${file.name} (${file.length() / 1024} KB)",
                        operationProgress = 1f
                    )
                }
                toast("Backup saved to ${BackupFileManager.getManualDisplayPath()}")
            } catch (e: Exception) {
                setOperationStatus(BackupOperationStatus.FAILED, "Backup failed: ${e.message}")
                toast("Backup failed: ${e.message}")
            }
        }
    }

    fun restoreFromBackup() {
        viewModelScope.launch {
            val file = BackupFileManager.findLatestBackup(context)
            if (file == null || !file.exists()) {
                toast("No backup found")
                return@launch
            }

            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Reading ${file.name}...", 0.2f)
            try {
                val json = withContext(Dispatchers.IO) { file.readText(Charsets.UTF_8) }
                val parseResult = BackupSerializer.fromJson(json)
                if (parseResult.file == null) {
                    setOperationStatus(BackupOperationStatus.FAILED, parseResult.error ?: "Invalid backup")
                    toast(parseResult.error ?: "Invalid backup file")
                    return@launch
                }

                setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Restoring data...", 0.6f)
                val ok = withContext(Dispatchers.IO) { applyRestore(parseResult.file) }

                if (ok) {
                    setOperationStatus(BackupOperationStatus.SUCCESS, "Restore complete", 1f)
                    toast("Restored ${parseResult.file.transactionCount} transactions")
                    _restoreComplete.value = true
                } else {
                    setOperationStatus(BackupOperationStatus.FAILED, "Restore failed")
                    toast("Restore failed")
                }
            } catch (e: Exception) {
                setOperationStatus(BackupOperationStatus.FAILED, "Error: ${e.message}")
                toast("Error restoring: ${e.message}")
            }
        }
    }

    fun restoreFromFileUri(ctx: Context, uri: Uri) {
        viewModelScope.launch {
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Reading backup file...", 0.2f)
            try {
                val json = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                } ?: throw Exception("Could not read file")

                val parseResult = BackupSerializer.fromJson(json)
                if (parseResult.file == null) {
                    setOperationStatus(BackupOperationStatus.FAILED, parseResult.error ?: "Invalid backup")
                    toast(parseResult.error ?: "Invalid backup file")
                    return@launch
                }

                setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Restoring data...", 0.6f)
                val ok = withContext(Dispatchers.IO) { applyRestore(parseResult.file) }

                if (ok) {
                    setOperationStatus(BackupOperationStatus.SUCCESS, "Restore complete", 1f)
                    toast("Restored ${parseResult.file.transactionCount} transactions")
                    _restoreComplete.value = true
                } else {
                    setOperationStatus(BackupOperationStatus.FAILED, "Restore failed")
                    toast("Restore failed")
                }
            } catch (e: Exception) {
                setOperationStatus(BackupOperationStatus.FAILED, "Error: ${e.message}")
                toast("Error restoring: ${e.message}")
            }
        }
    }

    fun shareBackup(ctx: Context) {
        viewModelScope.launch {
            setOperationStatus(BackupOperationStatus.IN_PROGRESS, "Generating backup...", 0.3f)
            try {
                val transactions = withContext(Dispatchers.IO) { dao.getAllCalculatedTransactions() }
                val categories = withContext(Dispatchers.IO) { dao.getAllCategories() }
                val balances = withContext(Dispatchers.IO) { dao.getCurrentBalances() }
                val backupId = UUID.randomUUID().toString().replace("-", "").take(12)

                val backupFile = BackupSerializer.serialize(
                    transactions = transactions,
                    categories = categories,
                    settings = BackupSettingsRecord(autoBackupEnabled = true),
                    bankBalance = balances.bankBalance,
                    walletBalance = balances.walletBalance,
                    cashBalance = balances.cashBalance,
                    backupId = backupId
                )

                val backupJson = BackupSerializer.toJson(backupFile)
                val manualFile = BackupFileManager.getManualBackupFile(ctx)
                withContext(Dispatchers.IO) {
                    manualFile.parentFile?.mkdirs()
                    manualFile.writeText(backupJson, Charsets.UTF_8)
                }

                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    ctx, "${ctx.packageName}.fileprovider", manualFile
                )

                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Money Minder Backup | ${transactions.size} transactions")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Money Minder backup file.\nTransactions: ${transactions.size}\nDate: ${java.util.Date()}")
                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = android.content.Intent.createChooser(sendIntent, "Share Backup").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(chooser)

                prefs.lastBackupAt = System.currentTimeMillis()
                prefs.lastBackupSizeBytes = manualFile.length()
                prefs.lastBackupStatus = "Manual backup shared"

                setOperationStatus(BackupOperationStatus.SUCCESS, "Backup ready to share", 1f)
                toast("Backup saved & shared from ${BackupFileManager.getManualDisplayPath()}")
            } catch (e: Exception) {
                setOperationStatus(BackupOperationStatus.FAILED, "Error: ${e.message}")
                toast("Could not create backup: ${e.message}")
            }
        }
    }

    fun hasLocalBackup(): Boolean = BackupFileManager.hasAnyBackup(context)

    private fun applyRestore(file: MoneyMinderBackupFile): Boolean {
        return try {
            dao.deleteAllData()
            val entities = file.transactions.mapNotNull { BackupSerializer.toTransactionEntity(it) }
            val categories = file.categories.map { BackupSerializer.toCategoryEntity(it) }
            dao.insertAllForRestore(entities)
            dao.insertCategoriesForRestore(categories)
            true
        } catch (e: Exception) { false }
    }

    fun acknowledgeRestore() { _restoreComplete.value = false }
    fun clearToast() { _toastMessage.value = null }
    fun clearOperationStatus() { _backupStatus.update { buildStatusFromPrefs() } }

    private fun toast(msg: String) { _toastMessage.value = msg }

    private fun setOperationStatus(status: BackupOperationStatus, message: String, progress: Float = 0f) {
        _backupStatus.update { it.copy(operationStatus = status, operationMessage = message, operationProgress = progress) }
    }

    private fun buildStatusFromPrefs(): BackupStatus {
        return BackupStatus(
            lastBackupAt = prefs.lastBackupAt,
            lastBackupSizeBytes = prefs.lastBackupSizeBytes,
            lastBackupStatus = prefs.lastBackupStatus
        )
    }
}
