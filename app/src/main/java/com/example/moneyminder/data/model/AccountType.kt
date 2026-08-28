package com.example.moneyminder.data.model

enum class AccountType(val displayName: String, val shortName: String) {
    BANK("Bank", "Bank"),
    WALLET("Wallet", "Wallet"),
    CASH("Cash", "Cash"),
    OVERALL("Overall", "Overall");

    companion object {
        val primaryAccounts = listOf(BANK, WALLET, CASH)

        fun fromString(value: String?): AccountType? {
            if (value == null) return null
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.equals(value, ignoreCase = true) 
            }
        }
    }
}
