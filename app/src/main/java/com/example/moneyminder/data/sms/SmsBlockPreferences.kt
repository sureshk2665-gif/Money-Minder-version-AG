package com.example.moneyminder.data.sms

import android.content.Context
import android.content.SharedPreferences

class SmsBlockPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mm_sms_block_prefs", Context.MODE_PRIVATE)

    fun getBlockedSenders(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_SENDERS, emptySet()) ?: emptySet()
    }

    fun blockSender(senderName: String) {
        val current = getBlockedSenders().toMutableSet()
        current.add(senderName.uppercase().trim())
        prefs.edit().putStringSet(KEY_BLOCKED_SENDERS, current).apply()
    }

    fun unblockSender(senderName: String) {
        val current = getBlockedSenders().toMutableSet()
        current.remove(senderName.uppercase().trim())
        prefs.edit().putStringSet(KEY_BLOCKED_SENDERS, current).apply()
    }

    fun isSenderBlocked(senderName: String): Boolean {
        return getBlockedSenders().contains(senderName.uppercase().trim())
    }

    companion object {
        private const val KEY_BLOCKED_SENDERS = "blocked_sms_senders"
    }
}
