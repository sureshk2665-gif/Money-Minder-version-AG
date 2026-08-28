package com.example.moneyminder.data.io

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.moneyminder.data.model.AccountBalances
import com.example.moneyminder.data.model.TransactionEntity
import com.example.moneyminder.data.model.TransactionType
import com.example.moneyminder.util.CurrencyFormatter
import com.example.moneyminder.util.DateTimeUtils
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun exportToPdf(
        context: Context,
        transactions: List<TransactionEntity>,
        balances: AccountBalances,
        fileName: String = "Money_Minder_Statement_${System.currentTimeMillis()}.pdf"
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, fileName)

        val document = PdfDocument()
        val pageWidth = 595 // Standard A4 points at 72dpi
        val pageHeight = 842
        var pageNumber = 1

        val titlePaint = Paint().apply {
            color = Color.rgb(20, 20, 25)
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(100, 100, 110)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerBoxPaint = Paint().apply {
            color = Color.rgb(240, 242, 245)
            style = Paint.Style.FILL
        }

        val headerTextPaint = Paint().apply {
            color = Color.rgb(40, 40, 45)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 30, 35)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val incomePaint = Paint().apply {
            color = Color.rgb(46, 125, 50) // Green
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val expensePaint = Paint().apply {
            color = Color.rgb(198, 40, 40) // Red
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val transferPaint = Paint().apply {
            color = Color.rgb(55, 71, 79) // Slate
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(220, 224, 230)
            strokeWidth = 1f
        }

        var currentY = 40f
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Header Function
        fun drawPageHeader(c: Canvas) {
            c.drawText("MONEY MINDER", 36f, 45f, titlePaint)
            c.drawText("Financial Statement · Generated on ${DateTimeUtils.formatDateTime(System.currentTimeMillis())}", 36f, 58f, subtitlePaint)
            c.drawLine(36f, 68f, (pageWidth - 36).toFloat(), 68f, linePaint)
        }

        drawPageHeader(canvas)
        currentY = 85f

        // Balance Summary Card Box on Page 1
        val summaryBoxRect = android.graphics.RectF(36f, currentY, (pageWidth - 36).toFloat(), currentY + 70f)
        canvas.drawRoundRect(summaryBoxRect, 8f, 8f, headerBoxPaint)

        val balanceLabelPaint = Paint().apply {
            color = Color.rgb(120, 120, 130)
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val balanceValPaint = Paint().apply {
            color = Color.rgb(20, 20, 25)
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        // 4 Columns: Overall, Bank, Wallet, Cash
        val colWidth = (pageWidth - 72) / 4f
        val col1X = 48f
        val col2X = col1X + colWidth
        val col3X = col2X + colWidth
        val col4X = col3X + colWidth

        canvas.drawText("OVERALL BALANCE", col1X, currentY + 25f, balanceLabelPaint)
        canvas.drawText(CurrencyFormatter.format(balances.overallBalance), col1X, currentY + 45f, balanceValPaint)

        canvas.drawText("BANK BALANCE", col2X, currentY + 25f, balanceLabelPaint)
        canvas.drawText(CurrencyFormatter.format(balances.bankBalance), col2X, currentY + 45f, balanceValPaint)

        canvas.drawText("WALLET BALANCE", col3X, currentY + 25f, balanceLabelPaint)
        canvas.drawText(CurrencyFormatter.format(balances.walletBalance), col3X, currentY + 45f, balanceValPaint)

        canvas.drawText("CASH BALANCE", col4X, currentY + 25f, balanceLabelPaint)
        canvas.drawText(CurrencyFormatter.format(balances.cashBalance), col4X, currentY + 45f, balanceValPaint)

        currentY += 90f

        // Table Header
        fun drawTableHeader(c: Canvas, y: Float) {
            c.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 24f, headerBoxPaint)
            c.drawText("DATE & TIME", 44f, y + 16f, headerTextPaint)
            c.drawText("CATEGORY / DETAILS", 160f, y + 16f, headerTextPaint)
            c.drawText("ACCOUNT / CONTEXT", 300f, y + 16f, headerTextPaint)
            c.drawText("AMOUNT", 430f, y + 16f, headerTextPaint)
            c.drawText("BALANCE", 500f, y + 16f, headerTextPaint)
        }

        drawTableHeader(canvas, currentY)
        currentY += 28f

        val rowHeight = 24f

        for (tx in transactions) {
            // Check if page overflow
            if (currentY + rowHeight > pageHeight - 40f) {
                // Draw footer on current page
                canvas.drawText("Page $pageNumber", (pageWidth / 2 - 15).toFloat(), pageHeight - 20f, subtitlePaint)
                document.finishPage(page)

                // Start new page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                drawPageHeader(canvas)
                currentY = 80f
                drawTableHeader(canvas, currentY)
                currentY += 28f
            }

            // Draw row separator
            canvas.drawLine(36f, currentY + rowHeight - 2f, (pageWidth - 36).toFloat(), currentY + rowHeight - 2f, linePaint)

            // Date & Time
            val dateStr = DateTimeUtils.formatDate(tx.timestamp)
            canvas.drawText(dateStr, 44f, currentY + 14f, textPaint)

            // Category / Description
            val catText = if (tx.category.length > 22) tx.category.take(20) + "..." else tx.category
            canvas.drawText(catText, 160f, currentY + 14f, textPaint)

            // Account context
            val accText = when (tx.type) {
                TransactionType.EXPENSE -> "Paid: ${tx.fromAccount?.displayName ?: ""}"
                TransactionType.INCOME -> "Recv: ${tx.toAccount?.displayName ?: ""}"
                TransactionType.TRANSFER -> "${tx.fromAccount?.shortName ?: ""} → ${tx.toAccount?.shortName ?: ""}"
            }
            canvas.drawText(accText, 300f, currentY + 14f, textPaint)

            // Amount
            val amountStr = when (tx.type) {
                TransactionType.EXPENSE -> "− ${CurrencyFormatter.format(tx.amount)}"
                TransactionType.INCOME -> "+ ${CurrencyFormatter.format(tx.amount)}"
                TransactionType.TRANSFER -> CurrencyFormatter.format(tx.amount)
            }
            val amountPaint = when (tx.type) {
                TransactionType.EXPENSE -> expensePaint
                TransactionType.INCOME -> incomePaint
                TransactionType.TRANSFER -> transferPaint
            }
            canvas.drawText(amountStr, 430f, currentY + 14f, amountPaint)

            // Historical Balance
            val balText = CurrencyFormatter.format(tx.balanceAfterPrimary)
            canvas.drawText(balText, 500f, currentY + 14f, textPaint)

            currentY += rowHeight
        }

        // Draw footer on last page
        canvas.drawText("Page $pageNumber", (pageWidth / 2 - 15).toFloat(), pageHeight - 20f, subtitlePaint)
        document.finishPage(page)

        val fos = FileOutputStream(outputFile)
        document.writeTo(fos)
        fos.flush()
        fos.close()
        document.close()

        return outputFile
    }
}
