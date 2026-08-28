package com.example.moneyminder

import com.example.moneyminder.data.backup.BackupSerializer
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.BackupFrequency
import com.example.moneyminder.data.model.BackupSettingsRecord
import com.example.moneyminder.data.model.CategoryEntity
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerTest {

    @Test
    fun testSerializationAndDeserializationRoundTrip() {
        val now = System.currentTimeMillis()
        val transactions = listOf(
            TransactionEntity(
                id = 1L,
                type = TransactionType.INCOME,
                amount = 50000.0,
                category = "Salary",
                fromAccount = null,
                toAccount = AccountType.BANK,
                timestamp = now - 100000,
                note = "Monthly salary"
            ),
            TransactionEntity(
                id = 2L,
                type = TransactionType.EXPENSE,
                amount = 1200.0,
                category = "Dining",
                fromAccount = AccountType.WALLET,
                toAccount = null,
                timestamp = now - 50000,
                note = "Dinner with friends"
            ),
            TransactionEntity(
                id = 3L,
                type = TransactionType.TRANSFER,
                amount = 5000.0,
                category = "ATM Withdrawal",
                fromAccount = AccountType.BANK,
                toAccount = AccountType.CASH,
                timestamp = now - 10000,
                note = "Self cash withdrawal"
            )
        )

        val categories = listOf(
            CategoryEntity(id = 1L, name = "Salary", type = TransactionType.INCOME, usageCount = 5, lastUsedTimestamp = now),
            CategoryEntity(id = 2L, name = "Dining", type = TransactionType.EXPENSE, usageCount = 12, lastUsedTimestamp = now)
        )

        val settings = BackupSettingsRecord(
            currency = "INR",
            currencySymbol = "₹",
            firstDayOfMonth = 1,
            autoBackupEnabled = true,
            backupFrequency = BackupFrequency.DAILY.name,
            connectedEmail = "testuser@gmail.com"
        )

        val backupFile = BackupSerializer.serialize(
            transactions = transactions,
            categories = categories,
            settings = settings,
            bankBalance = 45000.0,
            walletBalance = 8800.0,
            cashBalance = 5000.0,
            backupId = "test_backup_001"
        )

        val json = BackupSerializer.toJson(backupFile)
        assertTrue(json.isNotBlank())
        assertTrue(json.contains("test_backup_001"))
        assertTrue(json.contains("checksum"))

        val parseResult = BackupSerializer.fromJson(json)
        assertNull(parseResult.error)
        assertNotNull(parseResult.file)

        val parsed = parseResult.file!!
        assertEquals("test_backup_001", parsed.backupId)
        assertEquals(3, parsed.transactionCount)
        assertEquals(2, parsed.categoryCount)
        assertEquals(45000.0, parsed.bankBalanceSnapshot, 0.001)
        assertEquals(8800.0, parsed.walletBalanceSnapshot, 0.001)
        assertEquals(5000.0, parsed.cashBalanceSnapshot, 0.001)

        // Verify transfer transaction integrity
        val transferRecord = parsed.transactions.first { it.id == 3L }
        assertEquals("TRANSFER", transferRecord.type)
        assertEquals("BANK", transferRecord.fromAccount)
        assertEquals("CASH", transferRecord.toAccount)
        assertEquals(5000.0, transferRecord.amount, 0.001)

        val convertedEntity = BackupSerializer.toTransactionEntity(transferRecord)
        assertNotNull(convertedEntity)
        assertEquals(TransactionType.TRANSFER, convertedEntity!!.type)
        assertEquals(AccountType.BANK, convertedEntity.fromAccount)
        assertEquals(AccountType.CASH, convertedEntity.toAccount)
    }

    @Test
    fun testCorruptedChecksumFailsParsing() {
        val backupFile = BackupSerializer.serialize(
            transactions = emptyList(),
            categories = emptyList(),
            settings = BackupSettingsRecord(),
            bankBalance = 0.0,
            walletBalance = 0.0,
            cashBalance = 0.0,
            backupId = "checksum_test"
        )

        var json = BackupSerializer.toJson(backupFile)
        // Corrupt the checksum value
        json = json.replace("\"checksum\": \"", "\"checksum\": \"corrupted_checksum_")

        val parseResult = BackupSerializer.fromJson(json)
        assertNotNull(parseResult.error)
        assertNull(parseResult.file)
        assertTrue(parseResult.error!!.contains("integrity check failed"))
    }
}
