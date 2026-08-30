package com.example.moneyminder.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.moneyminder.data.model.AccountBalances
import com.example.moneyminder.data.model.AccountMovement
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.BudgetItem
import com.example.moneyminder.data.model.CategoryEntity
import com.example.moneyminder.data.model.CategorySpending
import com.example.moneyminder.data.model.DaySummary
import com.example.moneyminder.data.model.LentReturnItem
import com.example.moneyminder.data.model.MonthlySummary
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class TransactionDao(context: Context) {
    private val dbHelper = MoneyMinderDatabaseHelper(context)
    
    private val _dataVersion = MutableStateFlow(0L)
    val dataVersion: StateFlow<Long> = _dataVersion.asStateFlow()

    private fun notifyDataChanged() {
        _dataVersion.value = System.currentTimeMillis()
    }

    /** Public entry point for forced refresh — called after a backup restore. */
    fun notifyDataChangedPublic() = notifyDataChanged()

    /**
     * Retrieves all transactions chronologically with accurately computed
     * historical account balances immediately after each transaction.
     */
    @Synchronized
    fun getAllCalculatedTransactions(): List<TransactionEntity> {
        val rawList = mutableListOf<TransactionEntity>()
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            MoneyMinderDatabaseHelper.TABLE_TRANSACTIONS,
            null,
            null,
            null,
            null,
            null,
            "${MoneyMinderDatabaseHelper.COL_TIMESTAMP} ASC, ${MoneyMinderDatabaseHelper.COL_ID} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                rawList.add(cursorToTransaction(c))
            }
        }

        // Calculate running historical balances
        var runningBank = 0.0
        var runningWallet = 0.0
        var runningCash = 0.0

        val computedList = ArrayList<TransactionEntity>(rawList.size)

        for (tx in rawList) {
            var postPrimary = 0.0
            var postSecondary = 0.0

            when (tx.type) {
                TransactionType.EXPENSE -> {
                    when (tx.fromAccount) {
                        AccountType.BANK -> {
                            runningBank -= tx.amount
                            postPrimary = runningBank
                        }
                        AccountType.WALLET -> {
                            runningWallet -= tx.amount
                            postPrimary = runningWallet
                        }
                        AccountType.CASH -> {
                            runningCash -= tx.amount
                            postPrimary = runningCash
                        }
                        else -> {
                            runningBank -= tx.amount
                            postPrimary = runningBank
                        }
                    }
                }
                TransactionType.INCOME -> {
                    when (tx.toAccount) {
                        AccountType.BANK -> {
                            runningBank += tx.amount
                            postPrimary = runningBank
                        }
                        AccountType.WALLET -> {
                            runningWallet += tx.amount
                            postPrimary = runningWallet
                        }
                        AccountType.CASH -> {
                            runningCash += tx.amount
                            postPrimary = runningCash
                        }
                        else -> {
                            runningBank += tx.amount
                            postPrimary = runningBank
                        }
                    }
                }
                TransactionType.TRANSFER -> {
                    // Deduct from source
                    when (tx.fromAccount) {
                        AccountType.BANK -> { runningBank -= tx.amount; postPrimary = runningBank }
                        AccountType.WALLET -> { runningWallet -= tx.amount; postPrimary = runningWallet }
                        AccountType.CASH -> { runningCash -= tx.amount; postPrimary = runningCash }
                        else -> {}
                    }
                    // Add to destination
                    when (tx.toAccount) {
                        AccountType.BANK -> { runningBank += tx.amount; postSecondary = runningBank }
                        AccountType.WALLET -> { runningWallet += tx.amount; postSecondary = runningWallet }
                        AccountType.CASH -> { runningCash += tx.amount; postSecondary = runningCash }
                        else -> {}
                    }
                }
            }

            computedList.add(
                tx.copy(
                    balanceAfterPrimary = postPrimary,
                    balanceAfterSecondary = postSecondary
                )
            )
        }

        return computedList
    }

    /**
     * Get current balances for Bank, Wallet, Cash, and Overall.
     */
    @Synchronized
    fun getCurrentBalances(): AccountBalances {
        val all = getAllCalculatedTransactions()
        if (all.isEmpty()) return AccountBalances(0.0, 0.0, 0.0)

        var bank = 0.0
        var wallet = 0.0
        var cash = 0.0

        for (tx in all) {
            when (tx.type) {
                TransactionType.EXPENSE -> {
                    when (tx.fromAccount) {
                        AccountType.BANK -> bank -= tx.amount
                        AccountType.WALLET -> wallet -= tx.amount
                        AccountType.CASH -> cash -= tx.amount
                        else -> {}
                    }
                }
                TransactionType.INCOME -> {
                    when (tx.toAccount) {
                        AccountType.BANK -> bank += tx.amount
                        AccountType.WALLET -> wallet += tx.amount
                        AccountType.CASH -> cash += tx.amount
                        else -> {}
                    }
                }
                TransactionType.TRANSFER -> {
                    when (tx.fromAccount) {
                        AccountType.BANK -> bank -= tx.amount
                        AccountType.WALLET -> wallet -= tx.amount
                        AccountType.CASH -> cash -= tx.amount
                        else -> {}
                    }
                    when (tx.toAccount) {
                        AccountType.BANK -> bank += tx.amount
                        AccountType.WALLET -> wallet += tx.amount
                        AccountType.CASH -> cash += tx.amount
                        else -> {}
                    }
                }
            }
        }

        return AccountBalances(
            bankBalance = bank,
            walletBalance = wallet,
            cashBalance = cash
        )
    }

    /**
     * Insert a transaction and update category usage atomically.
     */
    @Synchronized
    fun insertTransaction(tx: TransactionEntity): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        var insertedId: Long = -1
        try {
            val values = ContentValues().apply {
                put(MoneyMinderDatabaseHelper.COL_TYPE, tx.type.name)
                put(MoneyMinderDatabaseHelper.COL_AMOUNT, tx.amount)
                put(MoneyMinderDatabaseHelper.COL_CATEGORY, tx.category.trim())
                put(MoneyMinderDatabaseHelper.COL_FROM_ACCOUNT, tx.fromAccount?.name)
                put(MoneyMinderDatabaseHelper.COL_TO_ACCOUNT, tx.toAccount?.name)
                put(MoneyMinderDatabaseHelper.COL_TIMESTAMP, tx.timestamp)
                put(MoneyMinderDatabaseHelper.COL_NOTE, tx.note.trim())
                put(MoneyMinderDatabaseHelper.COL_REF_NUMBER, tx.referenceNumber?.trim())
                put(MoneyMinderDatabaseHelper.COL_IS_PENDING, if (tx.isPending) 1 else 0)
            }
            insertedId = db.insert(MoneyMinderDatabaseHelper.TABLE_TRANSACTIONS, null, values)

            // Save or increment category if not transfer
            if (tx.type != TransactionType.TRANSFER && tx.category.isNotBlank()) {
                saveOrIncrementCategoryInternal(db, tx.category.trim(), tx.type, tx.timestamp)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        notifyDataChanged()
        return insertedId
    }

    /**
     * Update an existing transaction.
     */
    @Synchronized
    fun updateTransaction(tx: TransactionEntity): Boolean {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        var rows = 0
        try {
            val values = ContentValues().apply {
                put(MoneyMinderDatabaseHelper.COL_TYPE, tx.type.name)
                put(MoneyMinderDatabaseHelper.COL_AMOUNT, tx.amount)
                put(MoneyMinderDatabaseHelper.COL_CATEGORY, tx.category.trim())
                put(MoneyMinderDatabaseHelper.COL_FROM_ACCOUNT, tx.fromAccount?.name)
                put(MoneyMinderDatabaseHelper.COL_TO_ACCOUNT, tx.toAccount?.name)
                put(MoneyMinderDatabaseHelper.COL_TIMESTAMP, tx.timestamp)
                put(MoneyMinderDatabaseHelper.COL_NOTE, tx.note.trim())
                put(MoneyMinderDatabaseHelper.COL_REF_NUMBER, tx.referenceNumber?.trim())
                put(MoneyMinderDatabaseHelper.COL_IS_PENDING, if (tx.isPending) 1 else 0)
            }
            rows = db.update(
                MoneyMinderDatabaseHelper.TABLE_TRANSACTIONS,
                values,
                "${MoneyMinderDatabaseHelper.COL_ID} = ?",
                arrayOf(tx.id.toString())
            )

            if (tx.type != TransactionType.TRANSFER && tx.category.isNotBlank()) {
                saveOrIncrementCategoryInternal(db, tx.category.trim(), tx.type, tx.timestamp)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        notifyDataChanged()
        return rows > 0
    }

    /**
     * Delete transaction by ID.
     */
    @Synchronized
    fun deleteTransaction(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            MoneyMinderDatabaseHelper.TABLE_TRANSACTIONS,
            "${MoneyMinderDatabaseHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )
        notifyDataChanged()
        return rows > 0
    }

    /**
     * Delete all transactions and categories (Factory Reset).
     */
    @Synchronized
    fun deleteAllData() {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(MoneyMinderDatabaseHelper.TABLE_TRANSACTIONS, null, null)
            db.delete(MoneyMinderDatabaseHelper.TABLE_CATEGORIES, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyDataChanged()
    }

    /**
     * Batch insert transactions (e.g. from Excel/PDF import).
     */
    @Synchronized
    fun insertBatch(transactions: List<TransactionEntity>): Int {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        var count = 0
        try {
            for (tx in transactions) {
                val values = ContentValues().apply {
                    put(MoneyMinderDatabaseHelper.COL_TYPE, tx.type.name)
                    put(MoneyMinderDatabaseHelper.COL_AMOUNT, tx.amount)
                    put(MoneyMinderDatabaseHelper.COL_CATEGORY, tx.category.trim())
                    put(MoneyMinderDatabaseHelper.COL_FROM_ACCOUNT, tx.fromAccount?.name)
                    put(MoneyMinderDatabaseHelper.COL_TO_ACCOUNT, tx.toAccount?.name)
                    put(MoneyMinderDatabaseHelper.COL_TIMESTAMP, tx.timestamp)
                    put(MoneyMinderDatabaseHelper.COL_NOTE, tx.note.trim())
                    put(MoneyMinderDatabaseHelper.COL_REF_NUMBER, tx.referenceNumber?.trim())
                    put(MoneyMinderDatabaseHelper.COL_IS_PENDING, if (tx.isPending) 1 else 0)
                }
                db.insert(MoneyMinderDatabaseHelper.TABLE_TRANSACTIONS, null, values)
                if (tx.type != TransactionType.TRANSFER && tx.category.isNotBlank()) {
                    saveOrIncrementCategoryInternal(db, tx.category.trim(), tx.type, tx.timestamp)
                }
                count++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyDataChanged()
        return count
    }

    /**
     * Retrieve saved categories for Expense or Income, ordered by usage and last used.
     */
    @Synchronized
    fun getCategories(type: TransactionType): List<CategoryEntity> {
        val list = mutableListOf<CategoryEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MoneyMinderDatabaseHelper.TABLE_CATEGORIES,
            null,
            "${MoneyMinderDatabaseHelper.COL_CAT_TYPE} = ?",
            arrayOf(type.name),
            null,
            null,
            "${MoneyMinderDatabaseHelper.COL_CAT_USAGE_COUNT} DESC, ${MoneyMinderDatabaseHelper.COL_CAT_LAST_USED} DESC"
        )
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CAT_ID)
            val nameCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CAT_NAME)
            val typeCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CAT_TYPE)
            val usageCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CAT_USAGE_COUNT)
            val lastCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CAT_LAST_USED)

            while (c.moveToNext()) {
                list.add(
                    CategoryEntity(
                        id = c.getLong(idCol),
                        name = c.getString(nameCol),
                        type = TransactionType.fromString(c.getString(typeCol)),
                        usageCount = c.getInt(usageCol),
                        lastUsedTimestamp = c.getLong(lastCol)
                    )
                )
            }
        }
        return list
    }

    private fun saveOrIncrementCategoryInternal(
        db: android.database.sqlite.SQLiteDatabase,
        name: String,
        type: TransactionType,
        timestamp: Long
    ) {
        val cursor = db.query(
            MoneyMinderDatabaseHelper.TABLE_CATEGORIES,
            arrayOf(MoneyMinderDatabaseHelper.COL_CAT_ID, MoneyMinderDatabaseHelper.COL_CAT_USAGE_COUNT),
            "${MoneyMinderDatabaseHelper.COL_CAT_NAME} = ? COLLATE NOCASE AND ${MoneyMinderDatabaseHelper.COL_CAT_TYPE} = ?",
            arrayOf(name, type.name),
            null,
            null,
            null
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                val catId = c.getLong(0)
                val currentCount = c.getInt(1)
                val values = ContentValues().apply {
                    put(MoneyMinderDatabaseHelper.COL_CAT_USAGE_COUNT, currentCount + 1)
                    put(MoneyMinderDatabaseHelper.COL_CAT_LAST_USED, timestamp)
                }
                db.update(
                    MoneyMinderDatabaseHelper.TABLE_CATEGORIES,
                    values,
                    "${MoneyMinderDatabaseHelper.COL_CAT_ID} = ?",
                    arrayOf(catId.toString())
                )
            } else {
                val values = ContentValues().apply {
                    put(MoneyMinderDatabaseHelper.COL_CAT_NAME, name)
                    put(MoneyMinderDatabaseHelper.COL_CAT_TYPE, type.name)
                    put(MoneyMinderDatabaseHelper.COL_CAT_USAGE_COUNT, 1)
                    put(MoneyMinderDatabaseHelper.COL_CAT_LAST_USED, timestamp)
                }
                db.insert(MoneyMinderDatabaseHelper.TABLE_CATEGORIES, null, values)
            }
        }
    }

    /**
     * Check if a duplicate transaction already exists.
     */
    @Synchronized
    fun findDuplicate(
        referenceNumber: String?,
        timestamp: Long,
        amount: Double,
        type: TransactionType,
        account: AccountType?
    ): TransactionEntity? {
        val db = dbHelper.readableDatabase

        // First check by reference number if available
        if (!referenceNumber.isNullOrBlank()) {
            val cursor = db.query(
                MoneyMinderDatabaseHelper.TABLE_TRANSACTIONS,
                null,
                "${MoneyMinderDatabaseHelper.COL_REF_NUMBER} = ?",
                arrayOf(referenceNumber.trim()),
                null,
                null,
                null
            )
            cursor.use { c ->
                if (c.moveToFirst()) return cursorToTransaction(c)
            }
        }

        // Secondary check by amount, type, account and timestamp proximity (within 10 minutes)
        val timeWindow = 10 * 60 * 1000L // 10 minutes
        val minTime = timestamp - timeWindow
        val maxTime = timestamp + timeWindow

        val cursor = db.query(
            MoneyMinderDatabaseHelper.TABLE_TRANSACTIONS,
            null,
            "${MoneyMinderDatabaseHelper.COL_TYPE} = ? AND ABS(${MoneyMinderDatabaseHelper.COL_AMOUNT} - ?) < 0.01 AND ${MoneyMinderDatabaseHelper.COL_TIMESTAMP} BETWEEN ? AND ?",
            arrayOf(type.name, amount.toString(), minTime.toString(), maxTime.toString()),
            null,
            null,
            null
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                val tx = cursorToTransaction(c)
                if (account == null || tx.fromAccount == account || tx.toAccount == account) {
                    return tx
                }
            }
        }

        return null
    }

    /**
     * Get monthly summary for a given month and year.
     */
    @Synchronized
    fun getMonthlySummary(year: Int, month: Int): MonthlySummary {
        val startMillis = DateTimeUtils.getStartOfMonth(year, month)
        val endMillis = DateTimeUtils.getEndOfMonth(year, month)

        val allTransactions = getAllCalculatedTransactions()
        val monthTransactions = allTransactions.filter { it.timestamp in startMillis..endMillis }

        var incomeTotal = 0.0
        var expenseTotal = 0.0
        var transfersMoved = 0.0

        for (tx in monthTransactions) {
            when (tx.type) {
                TransactionType.INCOME -> incomeTotal += tx.amount
                TransactionType.EXPENSE -> expenseTotal += tx.amount
                TransactionType.TRANSFER -> transfersMoved += tx.amount
            }
        }

        return MonthlySummary(
            year = year,
            month = month,
            totalIncome = incomeTotal,
            totalExpense = expenseTotal,
            totalTransfersMoved = transfersMoved,
            dateRangeText = DateTimeUtils.getMonthDateRange(year, month)
        )
    }

    /**
     * Get category spending breakdown for Insights donut chart.
     * Note: Transfers are strictly excluded.
     */
    @Synchronized
    fun getCategorySpendings(year: Int, month: Int, accountFilter: AccountType): List<CategorySpending> {
        val startMillis = DateTimeUtils.getStartOfMonth(year, month)
        val endMillis = DateTimeUtils.getEndOfMonth(year, month)

        val all = getAllCalculatedTransactions()
        val expenses = all.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
            tx.timestamp in startMillis..endMillis &&
            (accountFilter == AccountType.OVERALL || tx.fromAccount == accountFilter)
        }

        val totalSpending = expenses.sumOf { it.amount }
        if (totalSpending <= 0.0) return emptyList()

        val grouped = expenses.groupBy { it.category.ifBlank { "Uncategorized" } }
        return grouped.map { (cat, txList) ->
            val catTotal = txList.sumOf { it.amount }
            CategorySpending(
                categoryName = cat,
                totalAmount = catTotal,
                percentage = ((catTotal / totalSpending) * 100).toFloat(),
                transactionCount = txList.size
            )
        }.sortedByDescending { it.totalAmount }
    }

    /**
     * Get transfer movement breakdown between accounts for selected month.
     */
    @Synchronized
    fun getAccountMovements(year: Int, month: Int): List<AccountMovement> {
        val startMillis = DateTimeUtils.getStartOfMonth(year, month)
        val endMillis = DateTimeUtils.getEndOfMonth(year, month)

        val all = getAllCalculatedTransactions()
        val transfers = all.filter { tx ->
            tx.type == TransactionType.TRANSFER &&
            tx.timestamp in startMillis..endMillis &&
            tx.fromAccount != null && tx.toAccount != null
        }

        val grouped = transfers.groupBy { Pair(it.fromAccount!!, it.toAccount!!) }
        return grouped.map { (pair, txList) ->
            AccountMovement(
                fromAccount = pair.first,
                toAccount = pair.second,
                totalTransferred = txList.sumOf { it.amount },
                transferCount = txList.size
            )
        }.sortedByDescending { it.totalTransferred }
    }

    /**
     * Get day-by-day summaries for Calendar grid.
     */
    @Synchronized
    fun getDaySummaries(year: Int, month: Int, accountFilter: AccountType): List<DaySummary> {
        val startMillis = DateTimeUtils.getStartOfMonth(year, month)
        val endMillis = DateTimeUtils.getEndOfMonth(year, month)

        val all = getAllCalculatedTransactions()
        val monthTransactions = all.filter { tx ->
            tx.timestamp in startMillis..endMillis &&
            (accountFilter == AccountType.OVERALL || tx.fromAccount == accountFilter || tx.toAccount == accountFilter)
        }

        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val list = ArrayList<DaySummary>(maxDays)
        for (day in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = DateTimeUtils.getStartOfDay(cal.timeInMillis)
            val dayEnd = DateTimeUtils.getEndOfDay(cal.timeInMillis)

            val dayTxs = monthTransactions.filter { it.timestamp in dayStart..dayEnd }
            var inc = 0.0
            var exp = 0.0
            var hasTrans = false

            for (t in dayTxs) {
                when (t.type) {
                    TransactionType.INCOME -> inc += t.amount
                    TransactionType.EXPENSE -> exp += t.amount
                    TransactionType.TRANSFER -> hasTrans = true
                }
            }

            list.add(
                DaySummary(
                    year = year,
                    month = month,
                    dayOfMonth = day,
                    dateMillis = cal.timeInMillis,
                    incomeTotal = inc,
                    expenseTotal = exp,
                    hasTransfers = hasTrans,
                    transactions = dayTxs
                )
            )
        }
        return list
    }

    /**
     * Retrieve all categories (both Expense and Income) — used for backup serialization.
     */
    @Synchronized
    fun getAllCategories(): List<CategoryEntity> {
        val list = mutableListOf<CategoryEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MoneyMinderDatabaseHelper.TABLE_CATEGORIES,
            null, null, null, null, null,
            "${MoneyMinderDatabaseHelper.COL_CAT_USAGE_COUNT} DESC"
        )
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CAT_ID)
            val nameCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CAT_NAME)
            val typeCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CAT_TYPE)
            val usageCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CAT_USAGE_COUNT)
            val lastCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CAT_LAST_USED)
            while (c.moveToNext()) {
                list.add(
                    CategoryEntity(
                        id = c.getLong(idCol),
                        name = c.getString(nameCol),
                        type = TransactionType.fromString(c.getString(typeCol)),
                        usageCount = c.getInt(usageCol),
                        lastUsedTimestamp = c.getLong(lastCol)
                    )
                )
            }
        }
        return list
    }

    /**
     * Batch-insert transactions during a restore operation.
     * Uses INSERT OR REPLACE so existing IDs are overwritten in Replace mode.
     */
    @Synchronized
    fun insertAllForRestore(transactions: List<TransactionEntity>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (tx in transactions) {
                val values = ContentValues().apply {
                    if (tx.id > 0) put(MoneyMinderDatabaseHelper.COL_ID, tx.id)
                    put(MoneyMinderDatabaseHelper.COL_TYPE, tx.type.name)
                    put(MoneyMinderDatabaseHelper.COL_AMOUNT, tx.amount)
                    put(MoneyMinderDatabaseHelper.COL_CATEGORY, tx.category.trim())
                    put(MoneyMinderDatabaseHelper.COL_FROM_ACCOUNT, tx.fromAccount?.name)
                    put(MoneyMinderDatabaseHelper.COL_TO_ACCOUNT, tx.toAccount?.name)
                    put(MoneyMinderDatabaseHelper.COL_TIMESTAMP, tx.timestamp)
                    put(MoneyMinderDatabaseHelper.COL_NOTE, tx.note)
                    put(MoneyMinderDatabaseHelper.COL_REF_NUMBER, tx.referenceNumber)
                    put(MoneyMinderDatabaseHelper.COL_IS_PENDING, if (tx.isPending) 1 else 0)
                }
                db.insertWithOnConflict(
                    MoneyMinderDatabaseHelper.TABLE_TRANSACTIONS,
                    null, values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyDataChanged()
    }

    /**
     * Batch-insert categories during a restore operation.
     * Skips categories that already exist (by name+type) in Merge mode.
     */
    @Synchronized
    fun insertCategoriesForRestore(categories: List<CategoryEntity>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (cat in categories) {
                val values = ContentValues().apply {
                    put(MoneyMinderDatabaseHelper.COL_CAT_NAME, cat.name)
                    put(MoneyMinderDatabaseHelper.COL_CAT_TYPE, cat.type.name)
                    put(MoneyMinderDatabaseHelper.COL_CAT_USAGE_COUNT, cat.usageCount)
                    put(MoneyMinderDatabaseHelper.COL_CAT_LAST_USED, cat.lastUsedTimestamp)
                }
                db.insertWithOnConflict(
                    MoneyMinderDatabaseHelper.TABLE_CATEGORIES,
                    null, values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyDataChanged()
    }

    // ─── Salary Budget ───

    @Synchronized
    fun getSalary(year: Int, month: Int): Double {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MoneyMinderDatabaseHelper.TABLE_SALARY,
            arrayOf(MoneyMinderDatabaseHelper.COL_SAL_AMOUNT),
            "${MoneyMinderDatabaseHelper.COL_SAL_YEAR} = ? AND ${MoneyMinderDatabaseHelper.COL_SAL_MONTH} = ?",
            arrayOf(year.toString(), month.toString()),
            null, null, null
        )
        cursor.use { c ->
            if (c.moveToFirst()) return c.getDouble(0)
        }
        return 0.0
    }

    @Synchronized
    fun setSalary(year: Int, month: Int, amount: Double) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MoneyMinderDatabaseHelper.COL_SAL_AMOUNT, amount)
            put(MoneyMinderDatabaseHelper.COL_SAL_YEAR, year)
            put(MoneyMinderDatabaseHelper.COL_SAL_MONTH, month)
        }
        db.insertWithOnConflict(
            MoneyMinderDatabaseHelper.TABLE_SALARY,
            null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        notifyDataChanged()
    }

    @Synchronized
    fun getBudgetItems(year: Int, month: Int): List<BudgetItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MoneyMinderDatabaseHelper.TABLE_BUDGET_ITEMS,
            null,
            "${MoneyMinderDatabaseHelper.COL_BUDGET_YEAR} = ? AND ${MoneyMinderDatabaseHelper.COL_BUDGET_MONTH} = ?",
            arrayOf(year.toString(), month.toString()),
            null, null,
            "${MoneyMinderDatabaseHelper.COL_BUDGET_CREATED} ASC"
        )
        val list = mutableListOf<BudgetItem>()
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(BudgetItem(
                    id = c.getLong(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_BUDGET_ID)),
                    purpose = c.getString(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_BUDGET_PURPOSE)),
                    amount = c.getDouble(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_BUDGET_AMOUNT)),
                    isDone = c.getInt(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_BUDGET_IS_DONE)) == 1,
                    year = c.getInt(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_BUDGET_YEAR)),
                    month = c.getInt(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_BUDGET_MONTH)),
                    createdAt = c.getLong(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_BUDGET_CREATED))
                ))
            }
        }
        return list
    }

    @Synchronized
    fun insertBudgetItem(item: BudgetItem): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MoneyMinderDatabaseHelper.COL_BUDGET_PURPOSE, item.purpose)
            put(MoneyMinderDatabaseHelper.COL_BUDGET_AMOUNT, item.amount)
            put(MoneyMinderDatabaseHelper.COL_BUDGET_IS_DONE, if (item.isDone) 1 else 0)
            put(MoneyMinderDatabaseHelper.COL_BUDGET_YEAR, item.year)
            put(MoneyMinderDatabaseHelper.COL_BUDGET_MONTH, item.month)
            put(MoneyMinderDatabaseHelper.COL_BUDGET_CREATED, item.createdAt)
        }
        val id = db.insert(MoneyMinderDatabaseHelper.TABLE_BUDGET_ITEMS, null, values)
        notifyDataChanged()
        return id
    }

    @Synchronized
    fun toggleBudgetItemDone(id: Long, isDone: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MoneyMinderDatabaseHelper.COL_BUDGET_IS_DONE, if (isDone) 1 else 0)
        }
        db.update(
            MoneyMinderDatabaseHelper.TABLE_BUDGET_ITEMS,
            values,
            "${MoneyMinderDatabaseHelper.COL_BUDGET_ID} = ?",
            arrayOf(id.toString())
        )
        notifyDataChanged()
    }

    @Synchronized
    fun deleteBudgetItem(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(
            MoneyMinderDatabaseHelper.TABLE_BUDGET_ITEMS,
            "${MoneyMinderDatabaseHelper.COL_BUDGET_ID} = ?",
            arrayOf(id.toString())
        )
        notifyDataChanged()
    }

    // ─── Lent & Returns ───

    @Synchronized
    fun getLentReturnItems(year: Int, month: Int): List<LentReturnItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MoneyMinderDatabaseHelper.TABLE_LENT_RETURN,
            null,
            "${MoneyMinderDatabaseHelper.COL_LR_YEAR} = ? AND ${MoneyMinderDatabaseHelper.COL_LR_MONTH} = ?",
            arrayOf(year.toString(), month.toString()),
            null, null,
            "${MoneyMinderDatabaseHelper.COL_LR_CREATED} ASC"
        )
        val list = mutableListOf<LentReturnItem>()
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(LentReturnItem(
                    id = c.getLong(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_LR_ID)),
                    personName = c.getString(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_LR_NAME)),
                    amount = c.getDouble(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_LR_AMOUNT)),
                    type = c.getString(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_LR_TYPE)),
                    isReturned = c.getInt(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_LR_IS_RETURNED)) == 1,
                    year = c.getInt(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_LR_YEAR)),
                    month = c.getInt(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_LR_MONTH)),
                    createdAt = c.getLong(c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_LR_CREATED))
                ))
            }
        }
        return list
    }

    @Synchronized
    fun insertLentReturnItem(item: LentReturnItem): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MoneyMinderDatabaseHelper.COL_LR_NAME, item.personName)
            put(MoneyMinderDatabaseHelper.COL_LR_AMOUNT, item.amount)
            put(MoneyMinderDatabaseHelper.COL_LR_TYPE, item.type)
            put(MoneyMinderDatabaseHelper.COL_LR_IS_RETURNED, if (item.isReturned) 1 else 0)
            put(MoneyMinderDatabaseHelper.COL_LR_YEAR, item.year)
            put(MoneyMinderDatabaseHelper.COL_LR_MONTH, item.month)
            put(MoneyMinderDatabaseHelper.COL_LR_CREATED, item.createdAt)
        }
        val id = db.insert(MoneyMinderDatabaseHelper.TABLE_LENT_RETURN, null, values)
        notifyDataChanged()
        return id
    }

    @Synchronized
    fun toggleLentReturnDone(id: Long, isReturned: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MoneyMinderDatabaseHelper.COL_LR_IS_RETURNED, if (isReturned) 1 else 0)
        }
        db.update(
            MoneyMinderDatabaseHelper.TABLE_LENT_RETURN,
            values,
            "${MoneyMinderDatabaseHelper.COL_LR_ID} = ?",
            arrayOf(id.toString())
        )
        notifyDataChanged()
    }

    @Synchronized
    fun deleteLentReturnItem(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(
            MoneyMinderDatabaseHelper.TABLE_LENT_RETURN,
            "${MoneyMinderDatabaseHelper.COL_LR_ID} = ?",
            arrayOf(id.toString())
        )
        notifyDataChanged()
    }

    private fun cursorToTransaction(c: Cursor): TransactionEntity {
        val idCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_ID)
        val typeCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_TYPE)
        val amountCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_AMOUNT)
        val catCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_CATEGORY)
        val fromCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_FROM_ACCOUNT)
        val toCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_TO_ACCOUNT)
        val timeCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_TIMESTAMP)
        val noteCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_NOTE)
        val refCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_REF_NUMBER)
        val pendCol = c.getColumnIndexOrThrow(MoneyMinderDatabaseHelper.COL_IS_PENDING)

        return TransactionEntity(
            id = c.getLong(idCol),
            type = TransactionType.fromString(c.getString(typeCol)),
            amount = c.getDouble(amountCol),
            category = c.getString(catCol) ?: "",
            fromAccount = AccountType.fromString(c.getString(fromCol)),
            toAccount = AccountType.fromString(c.getString(toCol)),
            timestamp = c.getLong(timeCol),
            note = c.getString(noteCol) ?: "",
            referenceNumber = c.getString(refCol),
            isPending = c.getInt(pendCol) == 1
        )
    }
}
