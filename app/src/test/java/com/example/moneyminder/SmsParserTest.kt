package com.example.moneyminder

import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.data.parser.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun testUnionBankAirtelExpense() {
        val sms = "Union Bank of India A/c *0531 Debited Rs:349.00 on 25-08-2026 10:00:38 by Mob Bk ref no 214034114011, Fvg: Airtel Avl Bal Rs:24163.65."
        val result = SmsParser.parseSingle(sms)
        assertNotNull(result)
        assertEquals(TransactionType.EXPENSE, result!!.type)
        assertEquals(349.0, result.amount, 0.01)
        assertEquals("Airtel", result.suggestedCategory)
        assertEquals(AccountType.BANK, result.suggestedAccount)
        assertEquals("214034114011", result.referenceNumber)
        assertEquals(24163.65, result.postBalance!!, 0.01)
    }

    @Test
    fun testPhonePeWalletExpense() {
        val sms = "You've paid Rs. 20 via PhonePe wallet. Not you? Call us on 022-68727374. Remaining balance: Rs. 106."
        val result = SmsParser.parseSingle(sms)
        assertNotNull(result)
        assertEquals(TransactionType.EXPENSE, result!!.type)
        assertEquals(20.0, result.amount, 0.01)
        assertEquals(AccountType.WALLET, result.suggestedAccount)
        assertEquals(106.0, result.postBalance!!, 0.01)
    }

    @Test
    fun testNeftInwardCreditPendingVerification() {
        val sms = "NEFT Inward to A/c No:XX3010, INR:1,770.96, Ref No:CITIN26715674428, Rem:ETERNAL LIMITED, Avl Bal INR 1,771.46 - Pending Verification - KVB"
        val result = SmsParser.parseSingle(sms)
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
        assertEquals(1770.96, result.amount, 0.01)
        assertEquals("ETERNAL LIMITED", result.suggestedCategory)
        assertEquals(AccountType.BANK, result.suggestedAccount)
        assertEquals("CITIN26715674428", result.referenceNumber)
        assertTrue(result.isPendingVerification)
    }

    @Test
    fun testBankCredit() {
        val sms = "A/c *0531 Credited for Rs:1000.00 on 19-07-2026 19:43:00 by Mob Bk ref no 426633691841 Avl Bal Rs:1414.57."
        val result = SmsParser.parseSingle(sms)
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
        assertEquals(1000.0, result.amount, 0.01)
        assertEquals(AccountType.BANK, result.suggestedAccount)
        assertEquals("426633691841", result.referenceNumber)
        assertEquals(1414.57, result.postBalance!!, 0.01)
    }
}
