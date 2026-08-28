package com.example.moneyminder.data.io

import android.content.Context
import android.net.Uri
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.ImportItem
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.util.DateTimeUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

object PdfImporter {

    fun importFromPdf(context: Context, uri: Uri): List<ImportItem> {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return emptyList()

        val rawText = StringBuilder()
        try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                rawText.append(line).append("\n")
            }
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        val text = rawText.toString()
        val items = mutableListOf<ImportItem>()

        // Search for lines with Date, Category, Amount
        val datePattern = Pattern.compile("(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}|\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})")
        val amountPattern = Pattern.compile("(?:[+−-]?\\s*[₹Rs.]*\\s*([0-9,]+(?:\\.[0-9]{1,2})?))")

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.length < 10) continue

            val dateMatcher = datePattern.matcher(trimmed)
            if (dateMatcher.find()) {
                val dateStr = dateMatcher.group(1) ?: continue
                val timestamp = DateTimeUtils.parseDate(dateStr) ?: continue

                // Check for amount
                val remainingLine = trimmed.substring(dateMatcher.end())
                val amountMatcher = amountPattern.matcher(remainingLine)
                if (amountMatcher.find()) {
                    val rawAmt = amountMatcher.group(1)?.replace(",", "")
                    val amount = rawAmt?.toDoubleOrNull() ?: continue
                    if (amount <= 0.0) continue

                    val isExpense = remainingLine.contains("−") || remainingLine.contains("-") || remainingLine.contains("Paid", ignoreCase = true)
                    val isIncome = remainingLine.contains("+") || remainingLine.contains("Recv", ignoreCase = true) || remainingLine.contains("Credit", ignoreCase = true)
                    val isTransfer = remainingLine.contains("→") || remainingLine.contains("Transfer", ignoreCase = true)

                    val type = when {
                        isTransfer -> TransactionType.TRANSFER
                        isIncome -> TransactionType.INCOME
                        else -> TransactionType.EXPENSE
                    }

                    val cat = remainingLine.substring(0, amountMatcher.start()).trim()
                        .replace("Paid:", "")
                        .replace("Recv:", "")
                        .trim()

                    items.add(
                        ImportItem(
                            type = type,
                            amount = amount,
                            category = cat.ifBlank { if (type == TransactionType.EXPENSE) "Expense" else if (type == TransactionType.INCOME) "Income" else "Transfer" },
                            fromAccount = if (type != TransactionType.INCOME) AccountType.BANK else null,
                            toAccount = if (type != TransactionType.EXPENSE) AccountType.BANK else null,
                            timestamp = timestamp
                        )
                    )
                }
            }
        }

        return items
    }
}
