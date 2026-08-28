package com.example.moneyminder.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.moneyminder.data.db.TransactionDao
import com.example.moneyminder.data.model.BackupFrequency
import com.example.moneyminder.data.model.BackupSettingsRecord
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * WorkManager background worker that performs scheduled automatic backups.
 * Respects Android battery and network constraints.
 * Skips backup if data has not changed since the last successful backup.
 */
class ScheduledBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = BackupPreferences(applicationContext)

        // Bail out if auto backup is not configured
        if (!prefs.isAutoBackupEnabled) return Result.success()
        if (!prefs.isConnected || (prefs.oauthAccessToken.isBlank() && prefs.refreshToken.isBlank())) return Result.success()

        val dao = TransactionDao(applicationContext)
        val transactions = dao.getAllCalculatedTransactions()

        // Skip if nothing changed since last successful backup
        val currentHash = BackupSerializer.computeDataHash(transactions)
        if (currentHash == prefs.lastSuccessfulBackupHash) {
            prefs.lastBackupStatus = "No changes since last backup"
            return Result.success()
        }

        return try {
            val categories = dao.getAllCategories()
            val balances = dao.getCurrentBalances()
            val backupId = UUID.randomUUID().toString().replace("-", "").take(12)

            val backupSettings = BackupSettingsRecord(
                autoBackupEnabled = prefs.isAutoBackupEnabled,
                backupFrequency = prefs.backupFrequency.name,
                connectedEmail = prefs.connectedEmail
            )

            val backupFile = BackupSerializer.serialize(
                transactions = transactions,
                categories = categories,
                settings = backupSettings,
                bankBalance = balances.bankBalance,
                walletBalance = balances.walletBalance,
                cashBalance = balances.cashBalance,
                backupId = backupId
            )

            val backupJson = BackupSerializer.toJson(backupFile)
            val manager = GoogleOAuthBackupManager(prefs)
            val uploadResult = manager.uploadBackup(backupJson, backupFile, isAutomatic = true)

            when (uploadResult) {
                is GoogleOAuthBackupManager.BackupResult.Success -> {
                    prefs.lastBackupAt = System.currentTimeMillis()
                    prefs.lastBackupId = backupId
                    prefs.lastBackupSizeBytes = uploadResult.sizeBytes
                    prefs.lastBackupStatus = "Automatic backup successful"
                    prefs.lastSuccessfulBackupHash = currentHash
                    Result.success()
                }
                is GoogleOAuthBackupManager.BackupResult.Failure -> {
                    prefs.lastBackupStatus = "Backup failed: ${uploadResult.message}"
                    Result.retry()
                }
            }


        } catch (e: Exception) {
            prefs.lastBackupStatus = "Backup error: ${e.message}"
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME_PERIODIC = "mm_scheduled_backup"
        private const val WORK_NAME_ONETIME  = "mm_immediate_backup"

        /** Schedule or reschedule the periodic backup job based on user frequency preference. */
        fun schedule(context: Context, frequency: BackupFrequency) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val (repeatInterval, unit) = when (frequency) {
                BackupFrequency.DAILY                  -> 1L to TimeUnit.DAYS
                BackupFrequency.WEEKLY                 -> 7L to TimeUnit.DAYS
                BackupFrequency.MONTHLY                -> 30L to TimeUnit.DAYS
                BackupFrequency.AFTER_EVERY_TRANSACTION -> 1L to TimeUnit.HOURS // capped at 1h debounce
            }

            val request = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(repeatInterval, unit)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /** Enqueue a one-time immediate backup (used for "Backup Now" button). */
        fun runOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ScheduledBackupWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /** Cancel all scheduled backup jobs. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        }
    }
}
