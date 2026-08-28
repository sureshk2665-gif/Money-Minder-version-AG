package com.example.moneyminder.data.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.moneyminder.data.model.BackupFrequency

/**
 * Manages all backup-related preferences.
 * Sensitive values (OAuth access token, refresh token, code verifier)
 * are stored securely in EncryptedSharedPreferences (AES-256 GCM).
 * Non-sensitive settings are in regular SharedPreferences for fast reads.
 */
class BackupPreferences(context: Context) {

    private val masterKeyAlias = try {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    } catch (e: Exception) {
        ""
    }

    private val encryptedPrefs: SharedPreferences = try {
        if (masterKeyAlias.isNotEmpty()) {
            EncryptedSharedPreferences.create(
                "mm_backup_secure",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } else {
            context.getSharedPreferences("mm_backup_secure_fb", Context.MODE_PRIVATE)
        }
    } catch (e: Exception) {
        context.getSharedPreferences("mm_backup_secure_fb", Context.MODE_PRIVATE)
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mm_backup_prefs", Context.MODE_PRIVATE)

    // ── OAuth Tokens (encrypted) ──────────────────────────────────────────────

    var oauthAccessToken: String
        get() = encryptedPrefs.getString(KEY_OAUTH_ACCESS_TOKEN, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_OAUTH_ACCESS_TOKEN, value).apply()

    var refreshToken: String
        get() = encryptedPrefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var oauthCodeVerifier: String
        get() = encryptedPrefs.getString(KEY_CODE_VERIFIER, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_CODE_VERIFIER, value).apply()

    // ── Direct Email Credentials (encrypted) ──────────────────────────────────

    var appPassword: String
        get() = encryptedPrefs.getString(KEY_APP_PASSWORD, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_APP_PASSWORD, value).apply()

    var smtpHost: String
        get() = prefs.getString(KEY_SMTP_HOST, "smtp.gmail.com") ?: "smtp.gmail.com"
        set(value) = prefs.edit().putString(KEY_SMTP_HOST, value).apply()

    var smtpPort: Int
        get() = prefs.getInt(KEY_SMTP_PORT, 465)
        set(value) = prefs.edit().putInt(KEY_SMTP_PORT, value).apply()

    var imapHost: String
        get() = prefs.getString(KEY_IMAP_HOST, "imap.gmail.com") ?: "imap.gmail.com"
        set(value) = prefs.edit().putString(KEY_IMAP_HOST, value).apply()

    var imapPort: Int
        get() = prefs.getInt(KEY_IMAP_PORT, 993)
        set(value) = prefs.edit().putInt(KEY_IMAP_PORT, value).apply()

    // ── Account info ──────────────────────────────────────────────────────────

    var connectedEmail: String
        get() = prefs.getString(KEY_CONNECTED_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CONNECTED_EMAIL, value).apply()

    var connectedName: String
        get() = prefs.getString(KEY_CONNECTED_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CONNECTED_NAME, value).apply()

    var isConnected: Boolean
        get() = prefs.getBoolean(KEY_IS_CONNECTED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_CONNECTED, value).apply()

    var customClientId: String
        get() = prefs.getString(KEY_CUSTOM_CLIENT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_CLIENT_ID, value).apply()

    var oauthRedirectUri: String
        get() = encryptedPrefs.getString(KEY_OAUTH_REDIRECT_URI, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_OAUTH_REDIRECT_URI, value).apply()

    // ── Auto backup settings ──────────────────────────────────────────────────

    var isAutoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, value).apply()

    var backupFrequency: BackupFrequency
        get() = try {
            BackupFrequency.valueOf(prefs.getString(KEY_FREQUENCY, BackupFrequency.WEEKLY.name) ?: BackupFrequency.WEEKLY.name)
        } catch (e: Exception) { BackupFrequency.WEEKLY }
        set(value) = prefs.edit().putString(KEY_FREQUENCY, value.name).apply()

    // ── Last backup info ──────────────────────────────────────────────────────

    var lastBackupAt: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_AT, value).apply()

    var lastBackupId: String
        get() = prefs.getString(KEY_LAST_BACKUP_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_BACKUP_ID, value).apply()

    var lastBackupSizeBytes: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_SIZE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_SIZE, value).apply()

    var lastBackupStatus: String
        get() = prefs.getString(KEY_LAST_BACKUP_STATUS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_BACKUP_STATUS, value).apply()

    /** Data hash of last successful backup — used to skip redundant uploads. */
    var lastSuccessfulBackupHash: String
        get() = prefs.getString(KEY_LAST_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_HASH, value).apply()

    // ── Clear all backup data ─────────────────────────────────────────────────

    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_OAUTH_ACCESS_TOKEN  = "oauth_access_token"
        private const val KEY_REFRESH_TOKEN       = "oauth_refresh_token"
        private const val KEY_CODE_VERIFIER       = "oauth_code_verifier"
        private const val KEY_APP_PASSWORD        = "app_password"
        private const val KEY_SMTP_HOST           = "smtp_host"
        private const val KEY_SMTP_PORT           = "smtp_port"
        private const val KEY_IMAP_HOST           = "imap_host"
        private const val KEY_IMAP_PORT           = "imap_port"
        private const val KEY_CONNECTED_EMAIL     = "connected_email"
        private const val KEY_CONNECTED_NAME      = "connected_name"
        private const val KEY_IS_CONNECTED        = "is_connected"
        private const val KEY_CUSTOM_CLIENT_ID    = "custom_client_id"
        private const val KEY_OAUTH_REDIRECT_URI  = "oauth_redirect_uri"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_FREQUENCY           = "backup_frequency"
        private const val KEY_LAST_BACKUP_AT      = "last_backup_at"
        private const val KEY_LAST_BACKUP_ID      = "last_backup_id"
        private const val KEY_LAST_BACKUP_SIZE    = "last_backup_size"
        private const val KEY_LAST_BACKUP_STATUS  = "last_backup_status"
        private const val KEY_LAST_HASH           = "last_backup_hash"
    }

}
