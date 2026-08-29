package com.example.moneyminder.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Google OAuth 2.0 PKCE Browser Authorization Manager.
 *
 * Implements RFC 8252 (OAuth 2.0 for Native Apps):
 * 1. Launches the default browser to Google's official sign-in page.
 * 2. Runs an embedded loopback listener on 127.0.0.1 to intercept the OAuth callback.
 * 3. Also supports reverse DNS custom URI scheme (moneyminder:// & com.googleusercontent.apps.*).
 * 4. Exchanges the authorization code + code verifier for OAuth access and refresh tokens.
 * 5. Automatically fetches the user's email and profile.
 */
object GoogleOAuthManager {

    const val DEFAULT_CLIENT_ID = "602264833180-877209u2k51g699g30k8q8c7b8g64e9e.apps.googleusercontent.com"
    const val SCOPES = "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile"

    private var activeServerJob: Job? = null
    private var activeServerSocket: ServerSocket? = null

    data class TokenResponse(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Long,
        val email: String,
        val name: String
    )

    fun getClientId(prefs: BackupPreferences): String {
        return prefs.customClientId.ifBlank { DEFAULT_CLIENT_ID }
    }

    fun getReversedClientIdScheme(clientId: String): String {
        val prefix = clientId.substringBefore(".apps.googleusercontent.com")
        return "com.googleusercontent.apps.$prefix"
    }

    /** Generate 32-byte cryptographically random code verifier. */
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /** Generate SHA-256 code challenge from verifier. */
    fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Start the browser-based sign-in flow.
     * Launches a local loopback server on 127.0.0.1 and opens the browser to Google's sign-in page.
     */
    fun startBrowserSignIn(
        context: Context,
        prefs: BackupPreferences,
        scope: CoroutineScope,
        onCodeReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Clean up previous server if running
        stopLoopbackServer()

        try {
            // Bind to loopback interface on random available port
            val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).apply {
                soTimeout = 180000 // 3-minute timeout
            }
            activeServerSocket = server
            val port = server.localPort

            val verifier = generateCodeVerifier()
            val challenge = generateCodeChallenge(verifier)
            prefs.oauthCodeVerifier = verifier

            val clientId = getClientId(prefs)
            val redirectUri = "http://127.0.0.1:$port/oauth2callback"
            prefs.oauthRedirectUri = redirectUri

            // Build Google authorization URL
            val authUrl = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth").buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", SCOPES)
                .appendQueryParameter("code_challenge", challenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("access_type", "offline")
                .appendQueryParameter("prompt", "consent select_account")
                .build()

            // Start background loopback listener
            activeServerJob = scope.launch(Dispatchers.IO) {
                try {
                    val socket = server.accept()
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val requestLine = reader.readLine() ?: ""

                    // Parse GET request line: GET /oauth2callback?code=... HTTP/1.1
                    val uriString = requestLine.split(" ").getOrNull(1) ?: ""
                    val uri = Uri.parse("http://127.0.0.1$uriString")
                    val code = uri.getQueryParameter("code")
                    val error = uri.getQueryParameter("error")

                    val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
                    if (!code.isNullOrBlank()) {
                        val successHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>Sign-in Successful</title>
                                <style>
                                    body { background-color: #0f1117; color: #ffffff; font-family: -apple-system, Roboto, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; padding: 20px; }
                                    .card { background: #1a1d26; border: 1px solid #2a2f3d; border-radius: 20px; padding: 32px 24px; max-width: 360px; box-shadow: 0 8px 30px rgba(0,0,0,0.5); }
                                    .icon { width: 56px; height: 56px; background: #0d2e18; color: #4caf50; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 28px; margin: 0 auto 16px; }
                                    h2 { margin: 0 0 8px; font-size: 20px; color: #4caf50; }
                                    p { color: #a0a6b5; font-size: 14px; line-height: 1.5; margin: 0 0 20px; }
                                    .btn { background: #42a5f5; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 12px; font-weight: bold; font-size: 14px; display: inline-block; }
                                </style>
                            </head>
                            <body>
                                <div class="card">
                                    <div class="icon">✓</div>
                                    <h2>Sign-in Successful!</h2>
                                    <p>Your Google account is now connected to Money Minder. You can return to the app.</p>
                                    <a class="btn" href="moneyminder://oauth/callback?code=$code">Return to Money Minder</a>
                                </div>
                                <script>
                                    setTimeout(function() {
                                        window.location.href = "moneyminder://oauth/callback?code=$code";
                                    }, 600);
                                </script>
                            </body>
                            </html>
                        """.trimIndent()

                        writer.write("HTTP/1.1 200 OK\r\n")
                        writer.write("Content-Type: text/html; charset=UTF-8\r\n")
                        writer.write("Content-Length: ${successHtml.toByteArray(Charsets.UTF_8).size}\r\n")
                        writer.write("Connection: close\r\n\r\n")
                        writer.write(successHtml)
                        writer.flush()

                        withContext(Dispatchers.Main) {
                            onCodeReceived(code)
                        }
                    } else {
                        val errorMsg = error ?: "Unknown error"
                        val errorHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Sign-in Cancelled</title></head>
                            <body style="background:#0f1117;color:#ef5350;text-align:center;padding:50px;font-family:sans-serif;">
                                <h2>Sign-in Cancelled</h2>
                                <p style="color:#a0a6b5;">$errorMsg</p>
                            </body>
                            </html>
                        """.trimIndent()

                        writer.write("HTTP/1.1 400 Bad Request\r\n")
                        writer.write("Content-Type: text/html; charset=UTF-8\r\n")
                        writer.write("Content-Length: ${errorHtml.toByteArray(Charsets.UTF_8).size}\r\n")
                        writer.write("Connection: close\r\n\r\n")
                        writer.write(errorHtml)
                        writer.flush()

                        withContext(Dispatchers.Main) {
                            onError(errorMsg)
                        }
                    }

                    socket.close()
                } catch (e: Exception) {
                    // Socket timed out or cancelled
                } finally {
                    try { server.close() } catch (ignored: Exception) {}
                }
            }

            // Launch default browser
            val intent = Intent(Intent.ACTION_VIEW, authUrl).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

        } catch (e: Exception) {
            onError("Could not start sign-in: ${e.message}")
        }
    }

    fun stopLoopbackServer() {
        try {
            activeServerSocket?.close()
            activeServerSocket = null
        } catch (ignored: Exception) {}
        activeServerJob?.cancel()
        activeServerJob = null
    }

    /** Exchange authorization code + code verifier for tokens. */
    suspend fun exchangeCodeForTokens(
        code: String,
        prefs: BackupPreferences
    ): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val verifier = prefs.oauthCodeVerifier
            val clientId = getClientId(prefs)

            val url = URL("https://oauth2.googleapis.com/token")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val clientSecret = prefs.customClientSecret
            val body = buildString {
                append("client_id=").append(URLEncoder.encode(clientId, "UTF-8"))
                if (clientSecret.isNotBlank()) {
                    append("&client_secret=").append(URLEncoder.encode(clientSecret, "UTF-8"))
                }
                append("&code=").append(URLEncoder.encode(code, "UTF-8"))
                append("&code_verifier=").append(URLEncoder.encode(verifier, "UTF-8"))
                append("&grant_type=authorization_code")
                val savedRedirectUri = prefs.oauthRedirectUri.ifBlank { "moneyminder://oauth/callback" }
                append("&redirect_uri=").append(URLEncoder.encode(savedRedirectUri, "UTF-8"))
            }

            conn.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                return@withContext Result.failure(Exception("OAuth exchange failed: $err"))
            }

            val responseText = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(responseText)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token", "")
            val expiresIn = json.optLong("expires_in", 3600L)

            // Fetch user's email & profile
            val (email, name) = fetchUserInfo(accessToken)

            Result.success(
                TokenResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = expiresIn,
                    email = email.ifBlank { "Google User" },
                    name = name
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fetch user email and display name from Google UserInfo endpoint. */
    suspend fun fetchUserInfo(accessToken: String): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.googleapis.com/oauth2/v2/userinfo")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode == 200) {
                val res = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(res)
                val email = json.optString("email", "")
                val name = json.optString("name", "")
                Pair(email, name)
            } else {
                Pair("", "")
            }
        } catch (e: Exception) {
            Pair("", "")
        }
    }

    /** Refresh expired access token using stored refresh token. */
    suspend fun refreshAccessToken(prefs: BackupPreferences): Result<String> = withContext(Dispatchers.IO) {
        val refreshToken = prefs.refreshToken
        if (refreshToken.isBlank()) return@withContext Result.failure(Exception("No refresh token available"))

        try {
            val clientId = getClientId(prefs)
            val url = URL("https://oauth2.googleapis.com/token")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val clientSecret = prefs.customClientSecret
            val body = buildString {
                append("client_id=").append(URLEncoder.encode(clientId, "UTF-8"))
                if (clientSecret.isNotBlank()) {
                    append("&client_secret=").append(URLEncoder.encode(clientSecret, "UTF-8"))
                }
                append("&refresh_token=").append(URLEncoder.encode(refreshToken, "UTF-8"))
                append("&grant_type=refresh_token")
            }

            conn.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val res = conn.inputStream.bufferedReader().readText()
                val newAccessToken = JSONObject(res).getString("access_token")
                prefs.oauthAccessToken = newAccessToken
                Result.success(newAccessToken)
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                Result.failure(Exception("Failed to refresh token: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
