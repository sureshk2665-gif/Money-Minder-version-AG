package com.example.moneyminder.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest

class MpinPreferences(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "mm_mpin_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val isMpinSet: Boolean
        get() = prefs.getString(KEY_PIN_HASH, null) != null

    fun setMpin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hashPin(pin)).apply()
    }

    fun verifyMpin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == hashPin(pin)
    }

    fun clearMpin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_SECURITY_Q1)
            .remove(KEY_SECURITY_A1)
            .remove(KEY_SECURITY_Q2)
            .remove(KEY_SECURITY_A2)
            .apply()
    }

    fun setSecurityQuestions(q1: String, a1: String, q2: String, a2: String) {
        prefs.edit()
            .putString(KEY_SECURITY_Q1, q1)
            .putString(KEY_SECURITY_A1, a1.trim().lowercase())
            .putString(KEY_SECURITY_Q2, q2)
            .putString(KEY_SECURITY_A2, a2.trim().lowercase())
            .apply()
    }

    fun getSecurityQuestion1(): String =
        prefs.getString(KEY_SECURITY_Q1, "") ?: ""

    fun getSecurityQuestion2(): String =
        prefs.getString(KEY_SECURITY_Q2, "") ?: ""

    fun verifySecurityAnswers(a1: String, a2: String): Boolean {
        val stored1 = prefs.getString(KEY_SECURITY_A1, null) ?: return false
        val stored2 = prefs.getString(KEY_SECURITY_A2, null) ?: return false
        return stored1 == a1.trim().lowercase() && stored2 == a2.trim().lowercase()
    }

    var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value).apply()

    var lockoutUntil: Long
        get() = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_LOCKOUT_UNTIL, value).apply()

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "mpin_hash"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val KEY_SECURITY_Q1 = "security_q1"
        private const val KEY_SECURITY_A1 = "security_a1"
        private const val KEY_SECURITY_Q2 = "security_q2"
        private const val KEY_SECURITY_A2 = "security_a2"

        val SECURITY_QUESTIONS = listOf(
            "What was your childhood nickname?",
            "What is the name of your first school?",
            "What was your first mobile phone brand?",
            "What is the name of your childhood best friend?",
            "What was the name of your first pet?",
            "What is the name of the street you grew up on?"
        )
    }
}
