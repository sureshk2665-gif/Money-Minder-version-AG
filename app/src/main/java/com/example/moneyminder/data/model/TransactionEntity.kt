package com.example.moneyminder.data.model

data class TransactionEntity(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val fromAccount: AccountType? = null,
    val toAccount: AccountType? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val referenceNumber: String? = null,
    val balanceAfterPrimary: Double = 0.0,
    val balanceAfterSecondary: Double = 0.0,
    val isPending: Boolean = false
) {
    val primaryAccount: AccountType?
        get() = when (type) {
            TransactionType.EXPENSE -> fromAccount
            TransactionType.INCOME -> toAccount
            TransactionType.TRANSFER -> fromAccount
        }

    val secondaryAccount: AccountType?
        get() = when (type) {
            TransactionType.TRANSFER -> toAccount
            else -> null
        }
}
