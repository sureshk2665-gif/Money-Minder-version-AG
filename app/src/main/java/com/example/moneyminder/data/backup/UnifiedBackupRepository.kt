package com.example.moneyminder.data.backup

import com.example.moneyminder.data.model.BackupMetadata
import com.example.moneyminder.data.model.MoneyMinderBackupFile

/**
 * Unified Backup Manager — delegates to Google Drive via OAuth 2.0.
 */
class UnifiedBackupRepository(
    private val prefs: BackupPreferences
) {
    private val driveManager = GoogleOAuthBackupManager(prefs)

    sealed class BackupResult {
        data class Success(val fileId: String, val sizeBytes: Long) : BackupResult()
        data class Failure(val message: String) : BackupResult()
    }

    sealed class RestoreResult {
        data class Success(val content: String) : RestoreResult()
        data class Failure(val message: String) : RestoreResult()
    }

    // ── Upload Backup ─────────────────────────────────────────────────────────

    suspend fun uploadBackup(
        backupJson: String,
        backupFile: MoneyMinderBackupFile,
        isAutomatic: Boolean
    ): BackupResult {
        if (prefs.oauthAccessToken.isBlank() && prefs.refreshToken.isBlank()) {
            return BackupResult.Failure("Not signed in. Please connect your Google account.")
        }

        val res = driveManager.uploadBackup(backupJson, backupFile, isAutomatic)
        return when (res) {
            is GoogleOAuthBackupManager.BackupResult.Success -> BackupResult.Success(res.driveFileId, res.sizeBytes)
            is GoogleOAuthBackupManager.BackupResult.Failure -> BackupResult.Failure(res.message)
        }
    }

    // ── List Backups ──────────────────────────────────────────────────────────

    suspend fun listBackups(): Result<List<BackupMetadata>> {
        if (prefs.oauthAccessToken.isBlank() && prefs.refreshToken.isBlank()) {
            return Result.failure(Exception("Not signed in"))
        }
        return driveManager.listBackups()
    }

    // ── Download Backup ───────────────────────────────────────────────────────

    suspend fun downloadBackup(fileId: String): RestoreResult {
        if (prefs.oauthAccessToken.isBlank() && prefs.refreshToken.isBlank()) {
            return RestoreResult.Failure("Not signed in")
        }

        val res = driveManager.downloadBackup(fileId)
        return when (res) {
            is GoogleOAuthBackupManager.RestoreResult.Success -> RestoreResult.Success(res.content)
            is GoogleOAuthBackupManager.RestoreResult.Failure -> RestoreResult.Failure(res.message)
        }
    }

    // ── Delete Backup ─────────────────────────────────────────────────────────

    suspend fun deleteBackup(fileId: String): Boolean {
        if (prefs.oauthAccessToken.isBlank() && prefs.refreshToken.isBlank()) return false
        return driveManager.deleteBackup(fileId)
    }
}
