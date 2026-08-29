package com.example.moneyminder.data.backup

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupFileManager {

    private const val ROOT_FOLDER = "Mm Backup"
    private const val AUTO_FOLDER = "Automatic"
    private const val MANUAL_FOLDER = "Manual"

    private const val PREFIX_AUTO = "MMBKAT"
    private const val PREFIX_MANUAL = "MMBKMN"
    private const val PREFIX_PDF = "MMBKPDF"
    private const val PREFIX_EXCEL = "MMBKXL"

    private const val BACKUP_EXT = ".mmbackup"
    private const val PDF_EXT = ".pdf"
    private const val EXCEL_EXT = ".xlsx"

    private val nameFormatter = SimpleDateFormat("ddMMyy", Locale.US)
    private val hourFormatter = SimpleDateFormat("HH", Locale.US)

    private fun timestamp(): String {
        val now = Date()
        return nameFormatter.format(now) + hourFormatter.format(now)
    }

    private fun getRootDir(context: Context): File {
        return File(context.getExternalFilesDir(null), ROOT_FOLDER).apply { mkdirs() }
    }

    private fun getAutoDir(context: Context): File {
        return File(getRootDir(context), AUTO_FOLDER).apply { mkdirs() }
    }

    private fun getManualDir(context: Context): File {
        return File(getRootDir(context), MANUAL_FOLDER).apply { mkdirs() }
    }

    fun getAutoBackupFile(context: Context): File {
        val dir = getAutoDir(context)
        return File(dir, "$PREFIX_AUTO${timestamp()}$BACKUP_EXT")
    }

    fun clearOldAutoBackups(context: Context) {
        val dir = getAutoDir(context)
        dir.listFiles()?.filter { it.name.startsWith(PREFIX_AUTO) && it.name.endsWith(BACKUP_EXT) }?.forEach { it.delete() }
    }

    fun getManualBackupFile(context: Context): File {
        val dir = getManualDir(context)
        return File(dir, "$PREFIX_MANUAL${timestamp()}$BACKUP_EXT")
    }

    fun getPdfExportFile(context: Context): File {
        return File(getRootDir(context), "$PREFIX_PDF${timestamp()}$PDF_EXT")
    }

    fun getExcelExportFile(context: Context): File {
        return File(getRootDir(context), "$PREFIX_EXCEL${timestamp()}$EXCEL_EXT")
    }

    fun findLatestBackup(context: Context): File? {
        val allBackups = mutableListOf<File>()
        getAutoDir(context).listFiles()?.filter { it.name.endsWith(BACKUP_EXT) }?.let { allBackups.addAll(it) }
        getManualDir(context).listFiles()?.filter { it.name.endsWith(BACKUP_EXT) }?.let { allBackups.addAll(it) }
        return allBackups.maxByOrNull { it.lastModified() }
    }

    fun hasAnyBackup(context: Context): Boolean {
        return findLatestBackup(context) != null
    }

    fun getDisplayPath(): String {
        return "Android/data/com.example.moneyminder/files/$ROOT_FOLDER"
    }

    fun getAutoDisplayPath(): String {
        return "${getDisplayPath()}/$AUTO_FOLDER"
    }

    fun getManualDisplayPath(): String {
        return "${getDisplayPath()}/$MANUAL_FOLDER"
    }
}
