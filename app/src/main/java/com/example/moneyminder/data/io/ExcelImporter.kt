package com.example.moneyminder.data.io

import android.content.Context
import android.net.Uri
import com.example.moneyminder.data.model.AccountType
import com.example.moneyminder.data.model.ImportItem
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.util.DateTimeUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.zip.ZipInputStream

object ExcelImporter {

    fun importFromFile(context: Context, uri: Uri): List<ImportItem> {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return emptyList()

        return try {
            val list = mutableListOf<ImportItem>()
            val zipIn = ZipInputStream(inputStream)
            var entry = zipIn.nextEntry

            val sharedStrings = mutableListOf<String>()
            var sheetXmlContent: String? = null

            // Read ZIP entries
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    val content = readStreamToString(zipIn)
                    sharedStrings.addAll(parseSharedStrings(content))
                } else if (entry.name == "xl/worksheets/sheet1.xml" || entry.name.endsWith(".xml")) {
                    sheetXmlContent = readStreamToString(zipIn)
                }
                entry = zipIn.nextEntry
            }

            if (sheetXmlContent != null) {
                list.addAll(parseSheetXml(sheetXmlContent, sharedStrings))
            } else {
                // If not zip/xlsx, try fallback CSV parsing
                inputStream.close()
                val freshStream = contentResolver.openInputStream(uri)
                if (freshStream != null) {
                    list.addAll(parseCsv(freshStream))
                    freshStream.close()
                }
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback CSV attempt
            try {
                val csvStream = contentResolver.openInputStream(uri) ?: return emptyList()
                val list = parseCsv(csvStream)
                csvStream.close()
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun readStreamToString(stream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val strings = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var currentText = StringBuilder()
            var insideT = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "t") {
                            insideT = true
                            currentText.clear()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideT) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "t") {
                            insideT = false
                            strings.add(currentText.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strings
    }

    private fun parseSheetXml(xml: String, sharedStrings: List<String>): List<ImportItem> {
        val items = mutableListOf<ImportItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var currentRowCells = mutableMapOf<Int, String>()
            var currentCellCol = 0
            var cellType = ""
            var currentCellValue = StringBuilder()
            var insideVal = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "row" -> {
                                currentRowCells.clear()
                            }
                            "c" -> {
                                val cellRef = parser.getAttributeValue(null, "r") ?: "A1"
                                currentCellCol = getColIndexFromRef(cellRef)
                                cellType = parser.getAttributeValue(null, "t") ?: ""
                                currentCellValue.clear()
                            }
                            "v", "t" -> {
                                insideVal = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideVal) {
                            currentCellValue.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "v", "t" -> {
                                insideVal = false
                            }
                            "c" -> {
                                val rawVal = currentCellValue.toString().trim()
                                val resolvedVal = if (cellType == "s") {
                                    val idx = rawVal.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                                } else {
                                    rawVal
                                }
                                currentRowCells[currentCellCol] = resolvedVal
                            }
                            "row" -> {
                                val item = parseRowCellsToItem(currentRowCells)
                                if (item != null) {
                                    items.add(item)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    private fun getColIndexFromRef(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }
        var result = 0
        for (ch in letters) {
            result = result * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return result - 1
    }

    private fun parseRowCellsToItem(cells: Map<Int, String>): ImportItem? {
        if (cells.isEmpty()) return null

        // Check if this row is a header or title
        val textValues = cells.values.joinToString(" ").lowercase()
        if (textValues.contains("money minder") || textValues.contains("overall balance") || textValues.contains("amount (inr)")) {
            return null
        }

        // Try extracting: Date, Time, Type, Amount, Category, Paid/Transfer From, Recv/Transfer To, Note
        var dateStr = cells[0] ?: ""
        var timeStr = cells[1] ?: ""
        var typeStr = cells[2] ?: ""
        var amountStr = cells[3] ?: ""
        var categoryStr = cells[4] ?: ""
        var fromStr = cells[5] ?: ""
        var toStr = cells[6] ?: ""
        var refStr = cells[8] ?: ""
        var noteStr = cells[9] ?: ""

        val amount = amountStr.replace(",", "").replace("₹", "").replace("Rs.", "").trim().toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val type = TransactionType.fromString(typeStr)
        val fromAccount = AccountType.fromString(fromStr)
        val toAccount = AccountType.fromString(toStr)

        val combinedDateStr = "$dateStr $timeStr".trim()
        val timestamp = DateTimeUtils.parseDate(combinedDateStr) ?: DateTimeUtils.parseDate(dateStr) ?: System.currentTimeMillis()

        return ImportItem(
            type = type,
            amount = amount,
            category = categoryStr.ifBlank { if (type == TransactionType.EXPENSE) "Expense" else if (type == TransactionType.INCOME) "Income" else "Transfer" },
            fromAccount = fromAccount ?: (if (type == TransactionType.EXPENSE || type == TransactionType.TRANSFER) AccountType.BANK else null),
            toAccount = toAccount ?: (if (type == TransactionType.INCOME || type == TransactionType.TRANSFER) AccountType.BANK else null),
            timestamp = timestamp,
            note = noteStr,
            referenceNumber = refStr.ifBlank { null }
        )
    }

    private fun parseCsv(stream: InputStream): List<ImportItem> {
        val items = mutableListOf<ImportItem>()
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val trimmed = line?.trim() ?: continue
            if (trimmed.isBlank()) continue

            val tokens = trimmed.split(",", "\t").map { it.trim().removeSurrounding("\"") }
            if (tokens.size >= 4) {
                val map = tokens.mapIndexed { index, s -> index to s }.toMap()
                val item = parseRowCellsToItem(map)
                if (item != null) items.add(item)
            }
        }
        return items
    }
}
