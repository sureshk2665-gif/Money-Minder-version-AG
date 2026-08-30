package com.example.moneyminder.data.model

data class AccountBalances(
    val bankBalance: Double = 0.0,
    val walletBalance: Double = 0.0,
    val cashBalance: Double = 0.0
) {
    val overallBalance: Double
        get() = bankBalance + walletBalance + cashBalance

    fun getBalance(account: AccountType): Double = when (account) {
        AccountType.BANK -> bankBalance
        AccountType.WALLET -> walletBalance
        AccountType.CASH -> cashBalance
        AccountType.OVERALL -> overallBalance
    }
}

data class MonthlySummary(
    val year: Int,
    val month: Int, // 1 to 12
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalTransfersMoved: Double = 0.0,
    val dateRangeText: String = ""
) {
    val netBalance: Double
        get() = totalIncome - totalExpense
}

data class DaySummary(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val dateMillis: Long,
    val incomeTotal: Double = 0.0,
    val expenseTotal: Double = 0.0,
    val hasTransfers: Boolean = false,
    val transactions: List<TransactionEntity> = emptyList()
) {
    val netTotal: Double
        get() = incomeTotal - expenseTotal
}

data class CategorySpending(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class AccountMovement(
    val fromAccount: AccountType,
    val toAccount: AccountType,
    val totalTransferred: Double,
    val transferCount: Int
)

data class SmsCandidate(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rawText: String,
    val type: TransactionType,
    val amount: Double,
    val suggestedCategory: String,
    val suggestedAccount: AccountType,
    val timestamp: Long,
    val referenceNumber: String? = null,
    val postBalance: Double? = null,
    val isPendingVerification: Boolean = false,
    val isDuplicate: Boolean = false,
    val duplicateReason: String? = null,
    val isSelected: Boolean = true
)

data class InboxSmsMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val body: String,
    val dateMillis: Long,
    val isFinancial: Boolean = false,
    val parsedCandidate: SmsCandidate? = null,
    val isDuplicate: Boolean = false,
    val duplicateReason: String? = null
)

data class BudgetItem(
    val id: Long = 0,
    val purpose: String,
    val amount: Double,
    val isDone: Boolean = false,
    val year: Int,
    val month: Int,
    val createdAt: Long = System.currentTimeMillis()
)

data class LentReturnItem(
    val id: Long = 0,
    val personName: String,
    val amount: Double,
    val type: String, // "LENT" or "RETURN"
    val isReturned: Boolean = false,
    val year: Int,
    val month: Int,
    val createdAt: Long = System.currentTimeMillis()
)

data class ImportItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val fromAccount: AccountType?,
    val toAccount: AccountType?,
    val timestamp: Long,
    val note: String = "",
    val referenceNumber: String? = null,
    val isDuplicate: Boolean = false,
    val isSelected: Boolean = true
)
