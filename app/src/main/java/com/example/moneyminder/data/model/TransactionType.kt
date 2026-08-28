package com.example.moneyminder.data.model

enum class TransactionType(val displayName: String, val sign: String) {
    EXPENSE("Expense", "−"),
    INCOME("Income", "+"),
    TRANSFER("Transfer", "⇄");

    companion object {
        fun fromString(value: String?): TransactionType {
            if (value == null) return EXPENSE
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.equals(value, ignoreCase = true) 
            } ?: EXPENSE
        }
    }
}
