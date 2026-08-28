package com.example.moneyminder

import com.example.moneyminder.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testWholeNumberFormatting() {
        assertEquals("₹6,800", CurrencyFormatter.format(6800.0))
        assertEquals("₹30,000", CurrencyFormatter.format(30000.0))
        assertEquals("₹2,500", CurrencyFormatter.format(2500.0))
        assertEquals("₹0", CurrencyFormatter.format(0.0))
    }

    @Test
    fun testDecimalFormatting() {
        assertEquals("₹1,770.96", CurrencyFormatter.format(1770.96))
        assertEquals("₹24,163.65", CurrencyFormatter.format(24163.65))
        assertEquals("₹1,414.57", CurrencyFormatter.format(1414.57))
    }
}
