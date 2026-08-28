package com.example.moneyminder.data.backup

import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.BackupCategoryRecord
import com.example.moneyminder.data.model.BackupSettingsRecord
import com.example.moneyminder.data.model.BackupTransactionRecord
import com.example.moneyminder.data.model.CategoryEntity
import com.example.moneyminder.data.model.MoneyMinderBackupFile
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Serializes all Money Minder data into a structured JSON backup and
 * deserializes it back with full integrity validation.
 *
 * Transfer integrity: A transfer is stored as ONE TransactionEntity record
 * with type=TRANSFER, fromAccount, and toAccount set. This single record is
 * preserved exactly — never split into separate income/expense records.
 */
object BackupSerializer {

    // ── Serialize ─────────────────────────────────────────────────────────────

    fun serialize(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        settings: BackupSettingsRecord,
        bankBalance: Double,
        walletBalance: Double,
        cashBalance: Double,
        backupId: String
    ): MoneyMinderBackupFile {
        return MoneyMinderBackupFile(
            version = 1,
            backupId = backupId,
            createdAt = System.currentTimeMillis(),
            deviceInfo = runCatching { android.os.Build.MODEL }.getOrNull() ?: "Android Device",
            transactionCount = transactions.size,
            categoryCount = categories.size,
            bankBalanceSnapshot = bankBalance,
            walletBalanceSnapshot = walletBalance,
            cashBalanceSnapshot = cashBalance,
            transactions = transactions.map { t ->
                BackupTransactionRecord(
                    id = t.id,
                    type = t.type.name,
                    amount = t.amount,
                    category = t.category,
                    fromAccount = t.fromAccount?.name,
                    toAccount = t.toAccount?.name,
                    timestamp = t.timestamp,
                    note = t.note,
                    referenceNumber = t.referenceNumber,
                    isPending = t.isPending
                )
            },
            categories = categories.map { c ->
                BackupCategoryRecord(
                    id = c.id,
                    name = c.name,
                    type = c.type.name,
                    usageCount = c.usageCount,
                    createdAt = c.lastUsedTimestamp
                )
            },
            settings = settings
        )
    }

    // ── JSON encoding ─────────────────────────────────────────────────────────

    fun toJson(file: MoneyMinderBackupFile): String {
        val root = JSONObject()
        root.put("version", file.version)
        root.put("backupId", file.backupId)
        root.put("createdAt", file.createdAt)
        root.put("deviceInfo", file.deviceInfo)
        root.put("transactionCount", file.transactionCount)
        root.put("categoryCount", file.categoryCount)
        root.put("bankBalanceSnapshot", file.bankBalanceSnapshot)
        root.put("walletBalanceSnapshot", file.walletBalanceSnapshot)
        root.put("cashBalanceSnapshot", file.cashBalanceSnapshot)

        val txArray = JSONArray()
        for (t in file.transactions) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("type", t.type)
            obj.put("amount", t.amount)
            obj.put("category", t.category)
            obj.put("fromAccount", t.fromAccount ?: JSONObject.NULL)
            obj.put("toAccount", t.toAccount ?: JSONObject.NULL)
            obj.put("timestamp", t.timestamp)
            obj.put("note", t.note)
            obj.put("referenceNumber", t.referenceNumber ?: JSONObject.NULL)
            obj.put("isPending", t.isPending)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val catArray = JSONArray()
        for (c in file.categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("type", c.type)
            obj.put("usageCount", c.usageCount)
            obj.put("createdAt", c.createdAt)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        val settingsObj = JSONObject()
        settingsObj.put("currency", file.settings.currency)
        settingsObj.put("currencySymbol", file.settings.currencySymbol)
        settingsObj.put("firstDayOfMonth", file.settings.firstDayOfMonth)
        settingsObj.put("autoBackupEnabled", file.settings.autoBackupEnabled)
        settingsObj.put("backupFrequency", file.settings.backupFrequency)
        settingsObj.put("connectedEmail", file.settings.connectedEmail)
        root.put("settings", settingsObj)

        // Append SHA-256 checksum of the data portion for integrity validation
        val dataChecksum = sha256(txArray.toString() + catArray.toString())
        root.put("checksum", dataChecksum)

        return root.toString(2)
    }

    // ── JSON decoding ─────────────────────────────────────────────────────────

    data class ParseResult(
        val file: MoneyMinderBackupFile?,
        val error: String?
    )

    fun fromJson(json: String): ParseResult {
        return try {
            val root = JSONObject(json)
            val version = root.optInt("version", 0)
            if (version < 1) return ParseResult(null, "Unsupported backup version: $version")

            val txArray = root.getJSONArray("transactions")
            val catArray = root.getJSONArray("categories")

            // Verify checksum if present
            val storedChecksum = root.optString("checksum", "")
            if (storedChecksum.isNotEmpty()) {
                val computed = sha256(txArray.toString() + catArray.toString())
                if (computed != storedChecksum) {
                    return ParseResult(null, "Backup integrity check failed. The file may be corrupted.")
                }
            }

            val transactions = mutableListOf<BackupTransactionRecord>()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                transactions.add(
                    BackupTransactionRecord(
                        id = obj.getLong("id"),
                        type = obj.getString("type"),
                        amount = obj.getDouble("amount"),
                        category = obj.optString("category", ""),
                        fromAccount = obj.optString("fromAccount", "").ifEmpty { null },
                        toAccount = obj.optString("toAccount", "").ifEmpty { null },
                        timestamp = obj.getLong("timestamp"),
                        note = obj.optString("note", ""),
                        referenceNumber = obj.optString("referenceNumber", "").ifEmpty { null },
                        isPending = obj.optBoolean("isPending", false)
                    )
                )
            }

            val categories = mutableListOf<BackupCategoryRecord>()
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                categories.add(
                    BackupCategoryRecord(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        type = obj.optString("type", "EXPENSE"),
                        usageCount = obj.optInt("usageCount", 0),
                        createdAt = obj.optLong("createdAt", 0L)
                    )
                )
            }

            val settingsObj = root.optJSONObject("settings") ?: JSONObject()
            val settings = BackupSettingsRecord(
                currency = settingsObj.optString("currency", "INR"),
                currencySymbol = settingsObj.optString("currencySymbol", "₹"),
                firstDayOfMonth = settingsObj.optInt("firstDayOfMonth", 1),
                autoBackupEnabled = settingsObj.optBoolean("autoBackupEnabled", false),
                backupFrequency = settingsObj.optString("backupFrequency", "WEEKLY"),
                connectedEmail = settingsObj.optString("connectedEmail", "")
            )

            ParseResult(
                MoneyMinderBackupFile(
                    version = version,
                    backupId = root.optString("backupId", "unknown"),
                    createdAt = root.optLong("createdAt", 0L),
                    deviceInfo = root.optString("deviceInfo", ""),
                    transactionCount = root.optInt("transactionCount", transactions.size),
                    categoryCount = root.optInt("categoryCount", categories.size),
                    bankBalanceSnapshot = root.optDouble("bankBalanceSnapshot", 0.0),
                    walletBalanceSnapshot = root.optDouble("walletBalanceSnapshot", 0.0),
                    cashBalanceSnapshot = root.optDouble("cashBalanceSnapshot", 0.0),
                    transactions = transactions,
                    categories = categories,
                    settings = settings
                ),
                null
            )
        } catch (e: Exception) {
            ParseResult(null, "Failed to parse backup file: ${e.message}")
        }
    }

    // ── Convert backup records → domain entities ──────────────────────────────

    fun toTransactionEntity(record: BackupTransactionRecord): TransactionEntity? {
        return try {
            TransactionEntity(
                id = record.id,
                type = TransactionType.valueOf(record.type),
                amount = record.amount,
                category = record.category,
                fromAccount = record.fromAccount?.let { runCatching { AccountType.valueOf(it) }.getOrNull() },
                toAccount = record.toAccount?.let { runCatching { AccountType.valueOf(it) }.getOrNull() },
                timestamp = record.timestamp,
                note = record.note,
                referenceNumber = record.referenceNumber,
                isPending = record.isPending
            )
        } catch (e: Exception) { null }
    }

    fun toCategoryEntity(record: BackupCategoryRecord): CategoryEntity {
        val txType = runCatching { TransactionType.valueOf(record.type) }.getOrElse { TransactionType.EXPENSE }
        return CategoryEntity(
            id = record.id,
            name = record.name,
            type = txType,
            usageCount = record.usageCount,
            lastUsedTimestamp = record.createdAt
        )
    }

    // ── Data hash for change detection ────────────────────────────────────────

    /** Returns a short hash of transaction count + latest timestamp. */
    fun computeDataHash(transactions: List<TransactionEntity>): String {
        val content = "${transactions.size}|${transactions.maxOfOrNull { it.timestamp } ?: 0L}"
        return sha256(content).take(16)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
