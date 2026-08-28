package com.example.moneyminder.data.parser

import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.SmsCandidate
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.util.DateTimeUtils
import java.util.regex.Pattern

object SmsParser {

    /**
     * Splits multiple pasted SMS messages and extracts structured candidates.
     */
    fun parseMultiple(text: String): List<SmsCandidate> {
        if (text.isBlank()) return emptyList()

        // Split text by common SMS message boundaries (double newlines or date/bank headers)
        val rawMessages = splitIntoIndividualMessages(text)
        val candidates = mutableListOf<SmsCandidate>()

        for (msg in rawMessages) {
            val trimmed = msg.trim()
            if (trimmed.length > 15) {
                val candidate = parseSingle(trimmed)
                if (candidate != null) {
                    candidates.add(candidate)
                }
            }
        }

        return candidates
    }

    private fun splitIntoIndividualMessages(text: String): List<String> {
        val lines = text.split("\n\n", "\r\n\r\n")
        if (lines.size > 1) return lines

        // If not separated by double newline, try regex splitting by common transaction keywords
        val messages = mutableListOf<String>()
        val current = StringBuilder()

        val rawLines = text.lines()
        for (line in rawLines) {
            if (line.isBlank()) {
                if (current.isNotBlank()) {
                    messages.add(current.toString())
                    current.clear()
                }
            } else {
                if (current.isNotEmpty()) current.append(" ")
                current.append(line.trim())
            }
        }
        if (current.isNotBlank()) {
            messages.add(current.toString())
        }

        return if (messages.isNotEmpty()) messages else listOf(text)
    }

    /**
     * Parses a single SMS text string.
     */
    fun parseSingle(sms: String): SmsCandidate? {
        val lower = sms.lowercase()

        // Filter out non-financial messages like OTPs or pure marketing
        val hasOtpKeyword = lower.contains("otp") || lower.contains("verification code") || lower.contains("one time password")
        val hasTransactionKeyword = lower.contains("debited") || lower.contains("credited") ||
                lower.contains("paid") || lower.contains("received") ||
                lower.contains("sent") || lower.contains("inward") ||
                lower.contains("transferred") || lower.contains("withdrawn")

        if (hasOtpKeyword && !hasTransactionKeyword) {
            return null
        }

        // Determine Transaction Type (Debit/Expense vs Credit/Income)
        val isExpense = lower.contains("debited") || lower.contains("paid") ||
                lower.contains("sent") || lower.contains("withdrawn") ||
                lower.contains("spent") || lower.contains("purchase") ||
                lower.contains("payment to") || lower.contains("dr")

        val isIncome = lower.contains("credited") || lower.contains("received") ||
                lower.contains("inward") || lower.contains("added") ||
                lower.contains("cr") || lower.contains("refund")

        if (!isExpense && !isIncome) {
            return null
        }

        val type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME

        // Extract Available / Remaining balance first so we don't confuse it with transaction amount
        val postBalance = extractAvailableBalance(sms)

        // Extract Transaction Amount (excluding the available balance part)
        val amount = extractTransactionAmount(sms, postBalance) ?: return null

        // Extract Account (Bank vs Wallet vs Cash)
        val account = extractAccount(sms)

        // Extract Payee / Merchant / Remitter / Category Name
        val categoryName = extractMerchantOrCategory(sms, type)

        // Extract Date & Time
        val timestamp = extractDateTime(sms) ?: System.currentTimeMillis()

        // Extract Reference Number (UPI, NEFT, IMPS, Mob Bk, etc.)
        val refNumber = extractReferenceNumber(sms)

        // Check if pending verification
        val isPending = lower.contains("pending") || lower.contains("verification")

        return SmsCandidate(
            rawText = sms,
            type = type,
            amount = amount,
            suggestedCategory = categoryName,
            suggestedAccount = account,
            timestamp = timestamp,
            referenceNumber = refNumber,
            postBalance = postBalance,
            isPendingVerification = isPending
        )
    }

    private fun extractAvailableBalance(sms: String): Double? {
        val patterns = listOf(
            Pattern.compile("(?i)(?:avl\\s*bal|available\\s*balance|remaining\\s*balance|bal\\s*is|bal:?)\\s*(?:rs\\.?|inr|₹)?\\s*:?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
            Pattern.compile("(?i)(?:bal|balance)\\s*:?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(sms)
            if (matcher.find()) {
                val numStr = matcher.group(1)?.replace(",", "")
                val num = numStr?.toDoubleOrNull()
                if (num != null) return num
            }
        }
        return null
    }

    private fun extractTransactionAmount(sms: String, postBalance: Double?): Double? {
        // Look for amount patterns near debit/credit/paid/received/inr/rs/₹
        val regexes = listOf(
            Pattern.compile("(?i)(?:debited\\s*(?:for)?|credited\\s*(?:for)?|paid|received|inward|transferred|spent|inr|rs\\.?|₹)\\s*(?:rs\\.?|inr|₹)?\\s*:?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
            Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*:?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
            Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr|₹|debited|credited)")
        )

        for (pattern in regexes) {
            val matcher = pattern.matcher(sms)
            while (matcher.find()) {
                val rawVal = matcher.group(1)?.replace(",", "")
                val num = rawVal?.toDoubleOrNull()
                if (num != null && num > 0.0) {
                    // Make sure it is not the post-transaction balance
                    if (postBalance != null && Math.abs(num - postBalance) < 0.001) {
                        continue
                    }
                    return num
                }
            }
        }
        return null
    }

    private fun extractAccount(sms: String): AccountType {
        val lower = sms.lowercase()
        if (lower.contains("wallet") || lower.contains("phonepe wallet") || lower.contains("paytm wallet") || lower.contains("amazon pay")) {
            return AccountType.WALLET
        }
        if (lower.contains("cash") || lower.contains("atm withdrawal")) {
            return AccountType.BANK
        }
        // Default for bank SMS
        return AccountType.BANK
    }

    private fun extractMerchantOrCategory(sms: String, type: TransactionType): String {
        // Examples: "Fvg: Airtel", "Rem:ETERNAL LIMITED", "to Swiggy", "via PhonePe", "at Starbucks"
        val specificPatterns = listOf(
            Pattern.compile("(?i)(?:remitter|rem|fvg|favou?ring|merchant|vpa)\\s*:?\\s*([A-Za-z0-9\\s&._-]{2,35})"),
            Pattern.compile("(?i)(?:paid\\s+to|spent\\s+on|transfer\\s+to|sent\\s+to)\\s*:?\\s*([A-Za-z0-9\\s&._-]{2,35})"),
            Pattern.compile("(?i)(?:via|by)\\s+([A-Za-z0-9\\s&._-]{2,25})\\s+(?:wallet|app|upi)"),
            Pattern.compile("(?i)\\bto\\s+([A-Za-z0-9\\s&._-]{2,25})")
        )

        for (pattern in specificPatterns) {
            val matcher = pattern.matcher(sms)
            if (matcher.find()) {
                var candidate = matcher.group(1)?.trim() ?: ""
                candidate = candidate.split(",", ";", "-", "Avl", "avl", "ref", "on", "by", "Ref").first().trim()
                if (candidate.length in 2..35 && 
                    !candidate.startsWith("a/c", ignoreCase = true) && 
                    !candidate.startsWith("ac", ignoreCase = true) && 
                    !candidate.startsWith("account", ignoreCase = true) &&
                    !candidate.equals("mob bk", ignoreCase = true) && 
                    !candidate.equals("upi", ignoreCase = true)) {
                    return candidate
                }
            }
        }

        // Check common merchants
        val lower = sms.lowercase()
        val commonKeywords = mapOf(
            "airtel" to "Airtel",
            "jio" to "Jio",
            "vi" to "Vi Recharge",
            "lic" to "LIC",
            "phonepe" to "PhonePe payment",
            "paytm" to "Paytm payment",
            "gpay" to "Google Pay",
            "swiggy" to "Swiggy",
            "zomato" to "Zomato",
            "amazon" to "Amazon",
            "flipkart" to "Flipkart",
            "uber" to "Uber",
            "ola" to "Ola",
            "salary" to "Salary",
            "electricity" to "Electricity Bill",
            "water" to "Water Bill",
            "rent" to "Rent",
            "vegetables" to "Vegetables",
            "groceries" to "Groceries"
        )

        for ((kw, cat) in commonKeywords) {
            if (lower.contains(kw)) {
                return cat
            }
        }

        return if (type == TransactionType.EXPENSE) "Expense" else "Bank Credit"
    }

    private fun extractDateTime(sms: String): Long? {
        val datePatterns = listOf(
            "\\b(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\s+\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:AM|PM|am|pm)?)\\b",
            "\\b(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{2,4}\\s*,?\\s*\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:AM|PM|am|pm)?)\\b",
            "\\b(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})\\b"
        )

        for (patStr in datePatterns) {
            val pattern = Pattern.compile(patStr)
            val matcher = pattern.matcher(sms)
            if (matcher.find()) {
                val matched = matcher.group(1) ?: continue
                val parsed = DateTimeUtils.parseDate(matched)
                if (parsed != null) return parsed
            }
        }
        return null
    }

    private fun extractReferenceNumber(sms: String): String? {
        val patterns = listOf(
            Pattern.compile("(?i)(?:ref\\s*no\\.?|ref(?:erence)?\\s*:?|rrn:?|txn\\s*id:?|upi\\s*ref:?|mob\\s*bk\\s*ref\\s*no)\\s*:?\\s*([A-Za-z0-9]{6,25})"),
            Pattern.compile("(?i)(?:ref\\s*#?\\s*)([A-Za-z0-9]{6,25})")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(sms)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        return null
    }
}
