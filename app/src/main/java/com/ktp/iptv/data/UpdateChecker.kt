package com.ktp.iptv.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Vérification des mises à jour in-app via l'API GitHub Releases.
 * Repo : mafiruss12/KTP-IPTV
 */
class UpdateChecker(
    private val context: Context,
    private val client: OkHttpClient = NetworkModule.createClient(timeoutSeconds = 20)
) {
    companion object {
        const val GITHUB_OWNER = "mafiruss12"
        const val GITHUB_REPO = "KTP-IPTV"
        private const val RELEASES_API =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Long,
        val changelog: String,
        val apkUrl: String,
        val htmlUrl: String
    )

    suspend fun checkForUpdate(currentVersionCode: Long): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_API)
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "").removePrefix("v")
                val bodyText = json.optString("body", "")
                val htmlUrl = json.optString("html_url", "")
                val assets = json.optJSONArray("assets") ?: return@withContext null

                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
                if (apkUrl.isNullOrBlank()) return@withContext null

                // versionCode depuis le tag si numérique, sinon 0
                val remoteCode = tag.filter { it.isDigit() }.toLongOrNull() ?: 0L
                if (remoteCode > 0 && remoteCode <= currentVersionCode) {
                    return@withContext null
                }
                // Si pas de versionCode numérique dans le tag, on propose quand même si tag différent

                UpdateInfo(
                    versionName = tag.ifBlank { "nouvelle version" },
                    versionCode = remoteCode,
                    changelog = bodyText.take(800),
                    apkUrl = apkUrl,
                    htmlUrl = htmlUrl
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Télécharge l'APK dans le cache et retourne le fichier.
     */
    suspend fun downloadApk(url: String, onProgress: (Int) -> Unit = {}): File? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body ?: return@withContext null
                    val total = body.contentLength()
                    val file = File(context.cacheDir, "ktp-iptv-update.apk")
                    file.outputStream().use { out ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8 * 1024)
                            var read: Int
                            var downloaded = 0L
                            while (input.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                                downloaded += read
                                if (total > 0) {
                                    onProgress(((downloaded * 100) / total).toInt())
                                }
                            }
                        }
                    }
                    file
                }
            } catch (_: Exception) {
                null
            }
        }

    fun installApk(file: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
