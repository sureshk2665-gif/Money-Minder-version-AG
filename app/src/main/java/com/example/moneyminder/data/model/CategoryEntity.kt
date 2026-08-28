package com.example.moneyminder.data.model

data class CategoryEntity(
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val usageCount: Int = 1,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)
