package com.example.moneyminder.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MoneyMinderDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        const val DATABASE_NAME = "moneyminder.db"
        const val DATABASE_VERSION = 2

        const val TABLE_TRANSACTIONS = "transactions"
        const val COL_ID = "id"
        const val COL_TYPE = "type"
        const val COL_AMOUNT = "amount"
        const val COL_CATEGORY = "category"
        const val COL_FROM_ACCOUNT = "from_account"
        const val COL_TO_ACCOUNT = "to_account"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_NOTE = "note"
        const val COL_REF_NUMBER = "reference_number"
        const val COL_IS_PENDING = "is_pending"

        const val TABLE_CATEGORIES = "categories"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"
        const val COL_CAT_TYPE = "type"
        const val COL_CAT_USAGE_COUNT = "usage_count"
        const val COL_CAT_LAST_USED = "last_used_timestamp"

        const val TABLE_BUDGET_ITEMS = "budget_items"
        const val COL_BUDGET_ID = "id"
        const val COL_BUDGET_SALARY = "salary"
        const val COL_BUDGET_PURPOSE = "purpose"
        const val COL_BUDGET_AMOUNT = "amount"
        const val COL_BUDGET_IS_DONE = "is_done"
        const val COL_BUDGET_YEAR = "year"
        const val COL_BUDGET_MONTH = "month"
        const val COL_BUDGET_CREATED = "created_at"

        const val TABLE_LENT_RETURN = "lent_return"
        const val COL_LR_ID = "id"
        const val COL_LR_NAME = "person_name"
        const val COL_LR_AMOUNT = "amount"
        const val COL_LR_TYPE = "type"
        const val COL_LR_IS_RETURNED = "is_returned"
        const val COL_LR_YEAR = "year"
        const val COL_LR_MONTH = "month"
        const val COL_LR_CREATED = "created_at"

        const val TABLE_SALARY = "salary"
        const val COL_SAL_ID = "id"
        const val COL_SAL_AMOUNT = "amount"
        const val COL_SAL_YEAR = "year"
        const val COL_SAL_MONTH = "month"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTransactionsTable = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TYPE TEXT NOT NULL,
                $COL_AMOUNT REAL NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_FROM_ACCOUNT TEXT,
                $COL_TO_ACCOUNT TEXT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_NOTE TEXT,
                $COL_REF_NUMBER TEXT,
                $COL_IS_PENDING INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NAME TEXT NOT NULL,
                $COL_CAT_TYPE TEXT NOT NULL,
                $COL_CAT_USAGE_COUNT INTEGER DEFAULT 1,
                $COL_CAT_LAST_USED INTEGER NOT NULL,
                UNIQUE($COL_CAT_NAME, $COL_CAT_TYPE)
            )
        """.trimIndent()

        db.execSQL(createTransactionsTable)
        db.execSQL(createCategoriesTable)
        db.execSQL("CREATE INDEX idx_trans_time ON $TABLE_TRANSACTIONS ($COL_TIMESTAMP)")
        db.execSQL("CREATE INDEX idx_trans_ref ON $TABLE_TRANSACTIONS ($COL_REF_NUMBER)")

        createBudgetTables(db)
    }

    private fun createBudgetTables(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_BUDGET_ITEMS (
                $COL_BUDGET_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BUDGET_PURPOSE TEXT NOT NULL,
                $COL_BUDGET_AMOUNT REAL NOT NULL,
                $COL_BUDGET_IS_DONE INTEGER DEFAULT 0,
                $COL_BUDGET_YEAR INTEGER NOT NULL,
                $COL_BUDGET_MONTH INTEGER NOT NULL,
                $COL_BUDGET_CREATED INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_LENT_RETURN (
                $COL_LR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LR_NAME TEXT NOT NULL,
                $COL_LR_AMOUNT REAL NOT NULL,
                $COL_LR_TYPE TEXT NOT NULL,
                $COL_LR_IS_RETURNED INTEGER DEFAULT 0,
                $COL_LR_YEAR INTEGER NOT NULL,
                $COL_LR_MONTH INTEGER NOT NULL,
                $COL_LR_CREATED INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_SALARY (
                $COL_SAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SAL_AMOUNT REAL NOT NULL,
                $COL_SAL_YEAR INTEGER NOT NULL,
                $COL_SAL_MONTH INTEGER NOT NULL,
                UNIQUE($COL_SAL_YEAR, $COL_SAL_MONTH)
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createBudgetTables(db)
        }
    }
}
