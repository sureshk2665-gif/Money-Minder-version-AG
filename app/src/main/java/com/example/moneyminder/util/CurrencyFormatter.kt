package com.example.moneyminder.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val wholeNumberFormat = DecimalFormat("##,##,##0").apply {
        maximumFractionDigits = 0
    }
    
    private val decimalFormat = DecimalFormat("##,##,##0.00").apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    /**
     * Formats amount with permanent ₹ symbol.
     * Whole numbers displayed without forced .00 (e.g., ₹6,800, ₹30,000).
     * Fractions displayed with exact decimals (e.g., ₹1,770.96, ₹24,163.65).
     */
    fun format(amount: Double): String {
        val absAmount = Math.abs(amount)
        val formattedNumber = if (absAmount % 1.0 == 0.0) {
            wholeNumberFormat.format(absAmount)
        } else {
            // Trim trailing zeros if any or format to 2 decimal places
            val formatted = decimalFormat.format(absAmount)
            if (formatted.endsWith(".00")) {
                wholeNumberFormat.format(absAmount)
            } else {
                formatted
            }
        }
        return "₹$formattedNumber"
    }

    /**
     * Format with sign (+ ₹30,000 or − ₹2,500 or ₹5,000)
     */
    fun formatWithSign(amount: Double, isIncome: Boolean, isExpense: Boolean): String {
        val base = format(amount)
        return when {
            isIncome -> "+ $base"
            isExpense -> "− $base"
            else -> base
        }
    }

    /**
     * Formats for input or raw display
     */
    fun formatPlain(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }
}
