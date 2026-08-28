package com.example.moneyminder.data.model

// ----- Backup Enums -----

enum class BackupFrequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    AFTER_EVERY_TRANSACTION("After Every Transaction")
}

enum class BackupType { MANUAL, AUTOMATIC }

enum class BackupRestoreMode { REPLACE, MERGE }

enum class BackupOperationStatus { IDLE, IN_PROGRESS, SUCCESS, FAILED }

// ----- Backup File Models (serialized to/from JSON) -----

data class BackupTransactionRecord(
    val id: Long,
    val type: String,          // EXPENSE / INCOME / TRANSFER
    val amount: Double,
    val category: String,
    val fromAccount: String?,
    val toAccount: String?,
    val timestamp: Long,
    val note: String,
    val referenceNumber: String?,
    val isPending: Boolean
)

data class BackupCategoryRecord(
    val id: Long,
    val name: String,
    val type: String,       // stored as type name string e.g. "EXPENSE", "INCOME"
    val usageCount: Int,
    val createdAt: Long     // maps to lastUsedTimestamp
)

data class BackupSettingsRecord(
    val currency: String = "INR",
    val currencySymbol: String = "₹",
    val firstDayOfMonth: Int = 1,
    val autoBackupEnabled: Boolean = false,
    val backupFrequency: String = BackupFrequency.WEEKLY.name,
    val connectedEmail: String = ""
)

/** Root structure that is JSON-serialized into the .mmbackup file. */
data class MoneyMinderBackupFile(
    val version: Int = 1,
    val backupId: String,
    val createdAt: Long,
    val deviceInfo: String,
    val transactionCount: Int,
    val categoryCount: Int,
    val bankBalanceSnapshot: Double,
    val walletBalanceSnapshot: Double,
    val cashBalanceSnapshot: Double,
    val transactions: List<BackupTransactionRecord>,
    val categories: List<BackupCategoryRecord>,
    val settings: BackupSettingsRecord
)

// ----- Backup Metadata (describes a remote backup in Google Drive) -----

data class BackupMetadata(
    val driveFileId: String,
    val backupId: String,
    val createdAt: Long,          // epoch millis
    val transactionCount: Int,
    val sizeBytes: Long,
    val type: BackupType,
    val version: Int,
    val bankSnapshot: Double,
    val walletSnapshot: Double,
    val cashSnapshot: Double
)

// ----- Live status shown in BackupSyncScreen -----

data class BackupStatus(
    val isConnected: Boolean = false,
    val connectedEmail: String = "",
    val lastBackupAt: Long = 0L,
    val lastBackupId: String = "",
    val lastBackupSizeBytes: Long = 0L,
    val lastBackupStatus: String = "",
    val isAutoEnabled: Boolean = false,
    val frequency: BackupFrequency = BackupFrequency.WEEKLY,
    val operationStatus: BackupOperationStatus = BackupOperationStatus.IDLE,
    val operationMessage: String = "",
    val operationProgress: Float = 0f,
    val hasClientId: Boolean = false,
    val clientIdPreview: String = ""
)

// ----- Restore Merge Preview -----

data class MergePreview(
    val newTransactionsToAdd: Int,
    val existingTransactionsKept: Int,
    val possibleDuplicates: Int,
    val transfersDetected: Int,
    val categoriesToAdd: Int,
    val bankBalanceAfter: Double,
    val walletBalanceAfter: Double,
    val cashBalanceAfter: Double
)
