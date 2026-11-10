package com.example.moneyminder.data.backup

import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.BACKUP_FORMAT_VERSION
import com.example.moneyminder.data.model.BackupBudgetItemRecord
import com.example.moneyminder.data.model.BackupHeldMoneyRecord
import com.example.moneyminder.data.model.BackupHeldMoneyTxRecord
import com.example.moneyminder.data.model.BackupLentReturnRecord
import com.example.moneyminder.data.model.BackupSalaryRecord
import com.example.moneyminder.data.model.BudgetItem
import com.example.moneyminder.data.model.HeldMoneyEntry
import com.example.moneyminder.data.model.HeldMoneyTransaction
import com.example.moneyminder.data.model.LentReturnItem
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
        backupId: String,
        budgetItems: List<BudgetItem> = emptyList(),
        lentReturns: List<LentReturnItem> = emptyList(),
        salaries: List<BackupSalaryRecord> = emptyList(),
        heldMoney: List<HeldMoneyEntry> = emptyList(),
        heldMoneyTransactions: List<HeldMoneyTransaction> = emptyList()
    ): MoneyMinderBackupFile {
        return MoneyMinderBackupFile(
            version = BACKUP_FORMAT_VERSION,
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
            settings = settings,
            budgetItems = budgetItems.map { b ->
                BackupBudgetItemRecord(
                    id = b.id,
                    purpose = b.purpose,
                    amount = b.amount,
                    isDone = b.isDone,
                    year = b.year,
                    month = b.month,
                    createdAt = b.createdAt
                )
            },
            lentReturns = lentReturns.map { l ->
                BackupLentReturnRecord(
                    id = l.id,
                    personName = l.personName,
                    amount = l.amount,
                    type = l.type,
                    isReturned = l.isReturned,
                    year = l.year,
                    month = l.month,
                    createdAt = l.createdAt
                )
            },
            salaries = salaries,
            heldMoney = heldMoney.map { h ->
                BackupHeldMoneyRecord(
                    id = h.id,
                    friendName = h.friendName,
                    totalAmount = h.totalAmount,
                    createdAt = h.createdAt
                )
            },
            heldMoneyTransactions = heldMoneyTransactions.map { t ->
                BackupHeldMoneyTxRecord(
                    id = t.id,
                    heldMoneyId = t.heldMoneyId,
                    type = t.type,
                    amount = t.amount,
                    note = t.note,
                    createdAt = t.createdAt
                )
            }
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

        val budgetArray = JSONArray()
        for (b in file.budgetItems) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("purpose", b.purpose)
            obj.put("amount", b.amount)
            obj.put("isDone", b.isDone)
            obj.put("year", b.year)
            obj.put("month", b.month)
            obj.put("createdAt", b.createdAt)
            budgetArray.put(obj)
        }
        root.put("budgetItems", budgetArray)

        val lentArray = JSONArray()
        for (l in file.lentReturns) {
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("personName", l.personName)
            obj.put("amount", l.amount)
            obj.put("type", l.type)
            obj.put("isReturned", l.isReturned)
            obj.put("year", l.year)
            obj.put("month", l.month)
            obj.put("createdAt", l.createdAt)
            lentArray.put(obj)
        }
        root.put("lentReturns", lentArray)

        val salaryArray = JSONArray()
        for (s in file.salaries) {
            val obj = JSONObject()
            obj.put("year", s.year)
            obj.put("month", s.month)
            obj.put("amount", s.amount)
            salaryArray.put(obj)
        }
        root.put("salaries", salaryArray)

        val heldArray = JSONArray()
        for (h in file.heldMoney) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("friendName", h.friendName)
            obj.put("totalAmount", h.totalAmount)
            obj.put("createdAt", h.createdAt)
            heldArray.put(obj)
        }
        root.put("heldMoney", heldArray)

        val heldTxArray = JSONArray()
        for (t in file.heldMoneyTransactions) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("heldMoneyId", t.heldMoneyId)
            obj.put("type", t.type)
            obj.put("amount", t.amount)
            obj.put("note", t.note)
            obj.put("createdAt", t.createdAt)
            heldTxArray.put(obj)
        }
        root.put("heldMoneyTransactions", heldTxArray)

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

            // Version 1 files carry none of these arrays; optJSONArray returns
            // null and each list simply stays empty.
            val budgetItems = mutableListOf<BackupBudgetItemRecord>()
            root.optJSONArray("budgetItems")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    budgetItems.add(
                        BackupBudgetItemRecord(
                            id = o.optLong("id", 0L),
                            purpose = o.optString("purpose", ""),
                            amount = o.optDouble("amount", 0.0),
                            isDone = o.optBoolean("isDone", false),
                            year = o.optInt("year", 0),
                            month = o.optInt("month", 0),
                            createdAt = o.optLong("createdAt", 0L)
                        )
                    )
                }
            }

            val lentReturns = mutableListOf<BackupLentReturnRecord>()
            root.optJSONArray("lentReturns")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    lentReturns.add(
                        BackupLentReturnRecord(
                            id = o.optLong("id", 0L),
                            personName = o.optString("personName", ""),
                            amount = o.optDouble("amount", 0.0),
                            type = o.optString("type", "LENT"),
                            isReturned = o.optBoolean("isReturned", false),
                            year = o.optInt("year", 0),
                            month = o.optInt("month", 0),
                            createdAt = o.optLong("createdAt", 0L)
                        )
                    )
                }
            }

            val salaries = mutableListOf<BackupSalaryRecord>()
            root.optJSONArray("salaries")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    salaries.add(
                        BackupSalaryRecord(
                            year = o.optInt("year", 0),
                            month = o.optInt("month", 0),
                            amount = o.optDouble("amount", 0.0)
                        )
                    )
                }
            }

            val heldMoney = mutableListOf<BackupHeldMoneyRecord>()
            root.optJSONArray("heldMoney")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    heldMoney.add(
                        BackupHeldMoneyRecord(
                            id = o.optLong("id", 0L),
                            friendName = o.optString("friendName", ""),
                            totalAmount = o.optDouble("totalAmount", 0.0),
                            createdAt = o.optLong("createdAt", 0L)
                        )
                    )
                }
            }

            val heldMoneyTx = mutableListOf<BackupHeldMoneyTxRecord>()
            root.optJSONArray("heldMoneyTransactions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    heldMoneyTx.add(
                        BackupHeldMoneyTxRecord(
                            id = o.optLong("id", 0L),
                            heldMoneyId = o.optLong("heldMoneyId", 0L),
                            type = o.optString("type", "SPENT"),
                            amount = o.optDouble("amount", 0.0),
                            note = o.optString("note", ""),
                            createdAt = o.optLong("createdAt", 0L)
                        )
                    )
                }
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
                    settings = settings,
                    budgetItems = budgetItems,
                    lentReturns = lentReturns,
                    salaries = salaries,
                    heldMoney = heldMoney,
                    heldMoneyTransactions = heldMoneyTx
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
