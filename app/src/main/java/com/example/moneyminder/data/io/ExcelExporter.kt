package com.example.moneyminder.data.io

import android.content.Context
import com.example.moneyminder.data.backup.BackupFileManager
import com.example.moneyminder.data.model.AccountBalances
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.util.CurrencyFormatter
import com.example.moneyminder.util.DateTimeUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    fun exportToExcel(
        context: Context,
        transactions: List<TransactionEntity>,
        balances: AccountBalances
    ): File {
        val outputFile = BackupFileManager.getExcelExportFile(context)
        outputFile.parentFile?.mkdirs()

        val zipOut = ZipOutputStream(FileOutputStream(outputFile))

        // 1. [Content_Types].xml
        zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
        zipOut.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // 2. _rels/.rels
        zipOut.putNextEntry(ZipEntry("_rels/.rels"))
        zipOut.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zipOut.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zipOut.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // 4. xl/workbook.xml
        zipOut.putNextEntry(ZipEntry("xl/workbook.xml"))
        zipOut.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="Money Minder Transactions" sheetId="1" r:id="rId1"/>
    </sheets>
</workbook>""".toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // 5. xl/worksheets/sheet1.xml
        val sheetXml = buildSheetXml(transactions, balances)
        zipOut.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        zipOut.write(sheetXml.toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        zipOut.close()
        return outputFile
    }

    private fun buildSheetXml(transactions: List<TransactionEntity>, balances: AccountBalances): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<sheetData>")

        var rowIndex = 1

        // Row 1: Title
        sb.append(createRow(rowIndex++, listOf("MONEY MINDER - FINANCIAL TRANSACTION REPORT")))
        sb.append(createRow(rowIndex++, listOf("Generated on: ${DateTimeUtils.formatDateTime(System.currentTimeMillis())}")))
        sb.append(createRow(rowIndex++, listOf(
            "Overall Balance: ${CurrencyFormatter.format(balances.overallBalance)}",
            "Bank: ${CurrencyFormatter.format(balances.bankBalance)}",
            "Wallet: ${CurrencyFormatter.format(balances.walletBalance)}",
            "Cash: ${CurrencyFormatter.format(balances.cashBalance)}"
        )))
        sb.append(createRow(rowIndex++, listOf(""))) // empty spacing row

        // Header Row
        val headers = listOf(
            "Date",
            "Time",
            "Type",
            "Amount (INR)",
            "Category / Name",
            "Paid From / Transfer From",
            "Received In / Transfer To",
            "Historical Balance",
            "Reference No",
            "Note"
        )
        sb.append(createRow(rowIndex++, headers))

        // Transaction Data Rows
        for (tx in transactions) {
            val dateStr = DateTimeUtils.formatDate(tx.timestamp)
            val timeStr = DateTimeUtils.formatTime(tx.timestamp)
            val typeStr = tx.type.displayName
            val amountStr = tx.amount.toString()
            val catStr = tx.category
            val fromStr = tx.fromAccount?.displayName ?: ""
            val toStr = tx.toAccount?.displayName ?: ""
            val balanceStr = when (tx.type) {
                TransactionType.EXPENSE, TransactionType.INCOME -> CurrencyFormatter.format(tx.balanceAfterPrimary)
                TransactionType.TRANSFER -> "${tx.fromAccount?.shortName}: ${CurrencyFormatter.format(tx.balanceAfterPrimary)} | ${tx.toAccount?.shortName}: ${CurrencyFormatter.format(tx.balanceAfterSecondary)}"
            }
            val refStr = tx.referenceNumber ?: ""
            val noteStr = tx.note

            val rowData = listOf(dateStr, timeStr, typeStr, amountStr, catStr, fromStr, toStr, balanceStr, refStr, noteStr)
            sb.append(createRow(rowIndex++, rowData))
        }

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun createRow(rowNum: Int, cells: List<String>): String {
        val sb = StringBuilder()
        sb.append("<row r=\"$rowNum\">")
        for (i in cells.indices) {
            val colLetter = getColumnLetter(i)
            val cellRef = "$colLetter$rowNum"
            val text = escapeXml(cells[i])
            sb.append("<c r=\"$cellRef\" t=\"inlineStr\"><is><t>$text</t></is></c>")
        }
        sb.append("</row>")
        return sb.toString()
    }

    private fun getColumnLetter(colIndex: Int): String {
        var temp = colIndex
        val sb = StringBuilder()
        while (temp >= 0) {
            sb.insert(0, ('A'.code + (temp % 26)).toChar())
            temp = temp / 26 - 1
        }
        return sb.toString()
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
