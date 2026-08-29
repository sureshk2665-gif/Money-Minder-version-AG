package com.example.moneyminder.data.parser

import android.content.Context
import android.net.Uri
import com.example.moneyminder.data.model.SmsCandidate

object SmsInboxReader {

    data class RawSms(
        val sender: String,
        val body: String,
        val dateMillis: Long
    )

    fun readFinancialSms(context: Context, limitDays: Int = 90): List<SmsCandidate> {
        val rawMessages = readInbox(context, limitDays)
        val candidates = mutableListOf<SmsCandidate>()

        for (raw in rawMessages) {
            val candidate = SmsParser.parseSingle(raw.body)
            if (candidate != null) {
                candidates.add(
                    candidate.copy(
                        timestamp = if (candidate.timestamp == 0L) raw.dateMillis else candidate.timestamp
                    )
                )
            }
        }

        return candidates.sortedByDescending { it.timestamp }
    }

    private fun readInbox(context: Context, limitDays: Int): List<RawSms> {
        val results = mutableListOf<RawSms>()
        val cutoff = System.currentTimeMillis() - (limitDays.toLong() * 24 * 60 * 60 * 1000)

        try {
            val uri = Uri.parse("content://sms/inbox")
            val cursor = context.contentResolver.query(
                uri,
                arrayOf("address", "body", "date"),
                "date > ?",
                arrayOf(cutoff.toString()),
                "date DESC"
            )

            cursor?.use {
                val addressIdx = it.getColumnIndexOrThrow("address")
                val bodyIdx = it.getColumnIndexOrThrow("body")
                val dateIdx = it.getColumnIndexOrThrow("date")

                while (it.moveToNext()) {
                    val sender = it.getString(addressIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: ""
                    val date = it.getLong(dateIdx)

                    if (body.length > 15) {
                        results.add(RawSms(sender, body, date))
                    }
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted
        } catch (_: Exception) {
            // Content provider not available
        }

        return results
    }
}
