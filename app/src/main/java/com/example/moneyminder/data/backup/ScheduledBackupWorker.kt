package com.example.moneyminder.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.moneyminder.data.db.TransactionDao
import com.example.moneyminder.data.model.BackupSettingsRecord
import java.io.File
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class ScheduledBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = BackupPreferences(applicationContext)
        val dao = TransactionDao(applicationContext)
        val transactions = dao.getAllCalculatedTransactions()

        val currentHash = BackupSerializer.computeDataHash(transactions)
        if (currentHash == prefs.lastSuccessfulBackupHash) {
            prefs.lastBackupStatus = "No changes since last backup"
            return Result.success()
        }

        return try {
            val categories = dao.getAllCategories()
            val balances = dao.getCurrentBalances()
            val backupId = UUID.randomUUID().toString().replace("-", "").take(12)

            val backupFile = BackupSerializer.serialize(
                transactions = transactions,
                categories = categories,
                settings = BackupSettingsRecord(autoBackupEnabled = true),
                bankBalance = balances.bankBalance,
                walletBalance = balances.walletBalance,
                cashBalance = balances.cashBalance,
                backupId = backupId
            )

            val backupJson = BackupSerializer.toJson(backupFile)
            val file = getBackupFile(applicationContext)
            file.parentFile?.mkdirs()
            file.writeText(backupJson, Charsets.UTF_8)

            prefs.lastBackupAt = System.currentTimeMillis()
            prefs.lastBackupSizeBytes = file.length()
            prefs.lastBackupStatus = "Automatic backup successful"
            prefs.lastSuccessfulBackupHash = currentHash
            Result.success()
        } catch (e: Exception) {
            prefs.lastBackupStatus = "Backup error: ${e.message}"
            Result.retry()
        }
    }

    companion object {
        private const val BACKUP_FILE_NAME = "moneyminder_backup.mmbackup"

        private val BACKUP_HOURS = intArrayOf(10, 14, 22)

        fun getBackupFile(context: Context): File {
            return File(context.filesDir, "backups/$BACKUP_FILE_NAME")
        }

        fun scheduleAll(context: Context) {
            val wm = WorkManager.getInstance(context)
            for (hour in BACKUP_HOURS) {
                val tag = "mm_backup_${hour}h"
                val delay = calculateDelayTo(hour)
                val request = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(
                    24, TimeUnit.HOURS,
                    30, TimeUnit.MINUTES
                )
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()

                wm.enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.UPDATE, request)
            }
        }

        fun cancelAll(context: Context) {
            val wm = WorkManager.getInstance(context)
            for (hour in BACKUP_HOURS) {
                wm.cancelUniqueWork("mm_backup_${hour}h")
            }
        }

        private fun calculateDelayTo(targetHour: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
