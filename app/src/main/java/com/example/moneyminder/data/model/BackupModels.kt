package com.example.moneyminder.data.model

enum class BackupType { MANUAL, AUTOMATIC }

enum class BackupOperationStatus { IDLE, IN_PROGRESS, SUCCESS, FAILED }

// ----- Backup File Models (serialized to/from JSON) -----

data class BackupTransactionRecord(
    val id: Long,
    val type: String,
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
    val type: String,
    val usageCount: Int,
    val createdAt: Long
)

data class BackupSettingsRecord(
    val currency: String = "INR",
    val currencySymbol: String = "₹",
    val firstDayOfMonth: Int = 1,
    val autoBackupEnabled: Boolean = true,
    val backupFrequency: String = "3x daily",
    val connectedEmail: String = ""
)

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

data class BackupStatus(
    val lastBackupAt: Long = 0L,
    val lastBackupSizeBytes: Long = 0L,
    val lastBackupStatus: String = "",
    val operationStatus: BackupOperationStatus = BackupOperationStatus.IDLE,
    val operationMessage: String = "",
    val operationProgress: Float = 0f
)
