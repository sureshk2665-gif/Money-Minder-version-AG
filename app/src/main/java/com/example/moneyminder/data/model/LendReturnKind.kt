package com.example.moneyminder.data.model

enum class LendReturnKind(
    val displayName: String,
    val transactionType: TransactionType,
    val categoryLabel: String,
    val budgetType: String
) {
    BORROWED("I Borrowed", TransactionType.INCOME, "Borrowed", "BORROWED"),
    LENT("I Lent", TransactionType.EXPENSE, "Lent", "LENT"),
    RETURN_PAID("I Repaid", TransactionType.EXPENSE, "Return Paid", "RETURN"),
    RETURN_RECEIVED("Got Repaid", TransactionType.INCOME, "Return Received", "RETURN");

    val isMoneyIn: Boolean
        get() = transactionType == TransactionType.INCOME
}
