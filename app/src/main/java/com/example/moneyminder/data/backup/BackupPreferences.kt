package com.example.moneyminder.data.backup

import android.content.Context
import android.content.SharedPreferences

class BackupPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mm_backup_prefs", Context.MODE_PRIVATE)

    var lastBackupAt: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_AT, value).apply()

    var lastBackupSizeBytes: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_SIZE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_SIZE, value).apply()

    var lastBackupStatus: String
        get() = prefs.getString(KEY_LAST_BACKUP_STATUS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_BACKUP_STATUS, value).apply()

    var lastSuccessfulBackupHash: String
        get() = prefs.getString(KEY_LAST_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_HASH, value).apply()

    companion object {
        private const val KEY_LAST_BACKUP_AT = "last_backup_at"
        private const val KEY_LAST_BACKUP_SIZE = "last_backup_size"
        private const val KEY_LAST_BACKUP_STATUS = "last_backup_status"
        private const val KEY_LAST_HASH = "last_backup_hash"
    }
}
