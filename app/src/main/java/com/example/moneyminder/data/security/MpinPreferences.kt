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
        prefs.edit().remove(KEY_PIN_HASH).apply()
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
    }
}
