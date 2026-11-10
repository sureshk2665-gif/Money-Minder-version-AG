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

data class BackupBudgetItemRecord(
    val id: Long,
    val purpose: String,
    val amount: Double,
    val isDone: Boolean,
    val year: Int,
    val month: Int,
    val createdAt: Long
)

data class BackupLentReturnRecord(
    val id: Long,
    val personName: String,
    val amount: Double,
    val type: String,
    val isReturned: Boolean,
    val year: Int,
    val month: Int,
    val createdAt: Long
)

data class BackupSalaryRecord(
    val year: Int,
    val month: Int,
    val amount: Double
)

data class BackupHeldMoneyRecord(
    val id: Long,
    val friendName: String,
    val totalAmount: Double,
    val createdAt: Long
)

data class BackupHeldMoneyTxRecord(
    val id: Long,
    val heldMoneyId: Long,
    val type: String,
    val amount: Double,
    val note: String,
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

/**
 * Version 2 adds budget items, lents and returns, salary and held money.
 * Version 1 files restore fine: the newer lists simply come back empty.
 */
data class MoneyMinderBackupFile(
    val version: Int = BACKUP_FORMAT_VERSION,
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
    val settings: BackupSettingsRecord,
    val budgetItems: List<BackupBudgetItemRecord> = emptyList(),
    val lentReturns: List<BackupLentReturnRecord> = emptyList(),
    val salaries: List<BackupSalaryRecord> = emptyList(),
    val heldMoney: List<BackupHeldMoneyRecord> = emptyList(),
    val heldMoneyTransactions: List<BackupHeldMoneyTxRecord> = emptyList()
)

const val BACKUP_FORMAT_VERSION = 2

data class BackupStatus(
    val lastBackupAt: Long = 0L,
    val lastBackupSizeBytes: Long = 0L,
    val lastBackupStatus: String = "",
    val operationStatus: BackupOperationStatus = BackupOperationStatus.IDLE,
    val operationMessage: String = "",
    val operationProgress: Float = 0f
)
