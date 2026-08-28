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
        const val DATABASE_VERSION = 1

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

        val createIndexes = """
            CREATE INDEX idx_trans_time ON $TABLE_TRANSACTIONS ($COL_TIMESTAMP);
            CREATE INDEX idx_trans_ref ON $TABLE_TRANSACTIONS ($COL_REF_NUMBER);
        """.trimIndent()

        db.execSQL(createTransactionsTable)
        db.execSQL(createCategoriesTable)
        db.execSQL("CREATE INDEX idx_trans_time ON $TABLE_TRANSACTIONS ($COL_TIMESTAMP)")
        db.execSQL("CREATE INDEX idx_trans_ref ON $TABLE_TRANSACTIONS ($COL_REF_NUMBER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations
    }
}
