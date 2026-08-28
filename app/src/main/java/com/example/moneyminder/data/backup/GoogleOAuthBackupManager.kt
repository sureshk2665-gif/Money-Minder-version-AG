package com.example.moneyminder.data.backup

import com.example.moneyminder.data.model.BackupMetadata
import com.example.moneyminder.data.model.BackupType
import com.example.moneyminder.data.model.MoneyMinderBackupFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Google Drive REST API client for backup storage authenticated via Google OAuth 2.0.
 *
 * Stores `.mmbackup` JSON backup files in a "MoneyMinder Backups" folder
 * on the user's Google Drive. Automatically refreshes expired OAuth tokens.
 */
class GoogleOAuthBackupManager(
    private val prefs: BackupPreferences
) {

    companion object {
        const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
        const val BACKUP_FOLDER_NAME = "MoneyMinder Backups"
        const val BACKUP_FILE_EXTENSION = ".mmbackup"
    }

    sealed class BackupResult {
        data class Success(val driveFileId: String, val sizeBytes: Long) : BackupResult()
        data class Failure(val message: String) : BackupResult()
    }

    sealed class RestoreResult {
        data class Success(val content: String) : RestoreResult()
        data class Failure(val message: String) : RestoreResult()
    }

    private suspend fun getValidAccessToken(): String? {
        val currentToken = prefs.oauthAccessToken
        if (currentToken.isNotBlank()) return currentToken
        val refreshRes = GoogleOAuthManager.refreshAccessToken(prefs)
        return refreshRes.getOrNull()
    }

    private fun getOrCreateFolder(token: String): String? {
        val query = "name='$BACKUP_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val searchUrl = URL("$DRIVE_API_BASE/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id)&spaces=drive")
        val conn = (searchUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 15000
            readTimeout = 15000
        }

        if (conn.responseCode == 200) {
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val files = json.optJSONArray("files")
            if (files != null && files.length() > 0) {
                return files.getJSONObject(0).getString("id")
            }
        }

        val createUrl = URL("$DRIVE_API_BASE/files")
        val createConn = (createUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15000
            readTimeout = 15000
        }

        val folderMeta = JSONObject().apply {
            put("name", BACKUP_FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }

        OutputStreamWriter(createConn.outputStream).use { it.write(folderMeta.toString()) }

        return if (createConn.responseCode in 200..201) {
            JSONObject(createConn.inputStream.bufferedReader().readText()).getString("id")
        } else {
            null
        }
    }

    // ── Upload Backup ─────────────────────────────────────────────────────────

    suspend fun uploadBackup(
        backupJson: String,
        backupFile: MoneyMinderBackupFile,
        isAutomatic: Boolean
    ): BackupResult = withContext(Dispatchers.IO) {
        var token = getValidAccessToken()
            ?: return@withContext BackupResult.Failure("Not signed in. Please connect your Google account.")

        try {
            val folderId = getOrCreateFolder(token)
                ?: return@withContext BackupResult.Failure("Could not create backup folder in Google Drive")

            val sdf = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
            val dateLabel = sdf.format(Date(backupFile.createdAt))
            val typeLabel = if (isAutomatic) "AUTO" else "MANUAL"
            val fileName = "MoneyMinder_${dateLabel}_${backupFile.backupId}$BACKUP_FILE_EXTENSION"

            val metadata = JSONObject().apply {
                put("name", fileName)
                put("parents", JSONArray().put(folderId))
                put("description", "Money Minder Backup | ${backupFile.transactionCount} tx | $typeLabel")
                put("appProperties", JSONObject().apply {
                    put("backupId", backupFile.backupId)
                    put("txCount", backupFile.transactionCount.toString())
                    put("type", typeLabel)
                    put("version", backupFile.version.toString())
                    put("bank", f(backupFile.bankBalanceSnapshot))
                    put("wallet", f(backupFile.walletBalanceSnapshot))
                    put("cash", f(backupFile.cashBalanceSnapshot))
                })
            }

            val boundary = "===moneyminder_drive_boundary==="
            val bodyBytes = buildDriveMultipart(boundary, metadata.toString(), backupJson)

            val uploadUrl = URL("$DRIVE_UPLOAD_BASE/files?uploadType=multipart&fields=id,size")
            var conn = openUploadConnection(uploadUrl, token, boundary)
            conn.outputStream.write(bodyBytes)

            if (conn.responseCode == 401) {
                val refreshedToken = GoogleOAuthManager.refreshAccessToken(prefs).getOrNull()
                if (refreshedToken != null) {
                    token = refreshedToken
                    conn = openUploadConnection(uploadUrl, token, boundary)
                    conn.outputStream.write(bodyBytes)
                }
            }

            if (conn.responseCode in 200..201) {
                val response = JSONObject(conn.inputStream.bufferedReader().readText())
                val fileId = response.getString("id")
                val sizeBytes = response.optLong("size", backupJson.toByteArray().size.toLong())
                BackupResult.Success(fileId, sizeBytes)
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP ${conn.responseCode}"
                BackupResult.Failure("Upload failed: $err")
            }
        } catch (e: Exception) {
            BackupResult.Failure("Upload error: ${e.message}")
        }
    }

    private fun openUploadConnection(url: URL, token: String, boundary: String): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            connectTimeout = 30000
            readTimeout = 30000
        }
    }

    private fun buildDriveMultipart(boundary: String, metadataJson: String, content: String): ByteArray {
        return buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json\r\n\r\n")
            append(content)
            append("\r\n--$boundary--")
        }.toByteArray(Charsets.UTF_8)
    }

    // ── List Backups ──────────────────────────────────────────────────────────

    suspend fun listBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        var token = getValidAccessToken()
            ?: return@withContext Result.failure(Exception("Not signed in"))

        try {
            val folderId = getOrCreateFolder(token)
                ?: return@withContext Result.failure(Exception("Could not access backup folder"))

            val query = "'$folderId' in parents and trashed=false"
            val fields = "files(id,name,size,createdTime,appProperties)"
            val url = URL("$DRIVE_API_BASE/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=$fields&orderBy=createdTime desc&pageSize=50")

            var conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (conn.responseCode == 401) {
                val refreshedToken = GoogleOAuthManager.refreshAccessToken(prefs).getOrNull()
                if (refreshedToken != null) {
                    token = refreshedToken
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Authorization", "Bearer $token")
                        connectTimeout = 15000
                        readTimeout = 15000
                    }
                }
            }

            if (conn.responseCode != 200) {
                return@withContext Result.failure(Exception("Drive list failed: HTTP ${conn.responseCode}"))
            }

            val response = conn.inputStream.bufferedReader().readText()
            val filesArray = JSONObject(response).optJSONArray("files") ?: JSONArray()

            val metadataList = mutableListOf<BackupMetadata>()
            for (i in 0 until filesArray.length()) {
                val meta = parseFileMeta(filesArray.getJSONObject(i))
                if (meta != null) metadataList.add(meta)
            }

            metadataList.sortByDescending { it.createdAt }
            Result.success(metadataList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFileMeta(file: JSONObject): BackupMetadata? {
        return try {
            val fileId = file.getString("id")
            val sizeBytes = file.optLong("size", 0L)
            val createdTime = file.optString("createdTime", "")

            val createdAt = try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(createdTime)?.time ?: 0L
            } catch (e: Exception) {
                try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(createdTime)?.time ?: 0L
                } catch (e2: Exception) { 0L }
            }

            val props = file.optJSONObject("appProperties")
            val backupId = props?.optString("backupId", fileId) ?: fileId
            val txCount = props?.optString("txCount", "0")?.toIntOrNull() ?: 0
            val typeStr = props?.optString("type", "MANUAL") ?: "MANUAL"
            val backupType = if (typeStr == "AUTO") BackupType.AUTOMATIC else BackupType.MANUAL
            val version = props?.optString("version", "1")?.toIntOrNull() ?: 1
            val bank = props?.optString("bank", "0")?.toDoubleOrNull() ?: 0.0
            val wallet = props?.optString("wallet", "0")?.toDoubleOrNull() ?: 0.0
            val cash = props?.optString("cash", "0")?.toDoubleOrNull() ?: 0.0

            BackupMetadata(
                driveFileId = fileId,
                backupId = backupId,
                createdAt = createdAt,
                transactionCount = txCount,
                sizeBytes = sizeBytes,
                type = backupType,
                version = version,
                bankSnapshot = bank,
                walletSnapshot = wallet,
                cashSnapshot = cash
            )
        } catch (e: Exception) { null }
    }

    // ── Download Backup Content ───────────────────────────────────────────────

    suspend fun downloadBackup(driveFileId: String): RestoreResult = withContext(Dispatchers.IO) {
        var token = getValidAccessToken()
            ?: return@withContext RestoreResult.Failure("Not signed in")

        try {
            val url = URL("$DRIVE_API_BASE/files/$driveFileId?alt=media")
            var conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 15000
                readTimeout = 30000
            }

            if (conn.responseCode == 401) {
                val refreshedToken = GoogleOAuthManager.refreshAccessToken(prefs).getOrNull()
                if (refreshedToken != null) {
                    token = refreshedToken
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Authorization", "Bearer $token")
                        connectTimeout = 15000
                        readTimeout = 30000
                    }
                }
            }

            if (conn.responseCode != 200) {
                return@withContext RestoreResult.Failure("Download failed: HTTP ${conn.responseCode}")
            }

            val content = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            RestoreResult.Success(content)
        } catch (e: Exception) {
            RestoreResult.Failure("Download error: ${e.message}")
        }
    }

    // ── Delete Backup ─────────────────────────────────────────────────────────

    suspend fun deleteBackup(driveFileId: String): Boolean = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext false
        try {
            val url = URL("$DRIVE_API_BASE/files/$driveFileId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 10000
                readTimeout = 10000
            }
            conn.responseCode in 200..204
        } catch (e: Exception) { false }
    }

    private fun f(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() else "%.2f".format(d)
}
