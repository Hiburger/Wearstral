package dev.wearstral.update

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val apkUrl: String?,
    val htmlUrl: String,
    val publishedAt: String
)

object UpdateChecker {
    private const val GITHUB_OWNER = "Hiburger"
    private const val GITHUB_REPO = "Wearstral"
    private const val API_URL =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    sealed class UpdateState {
        object Idle : UpdateState()
        object Checking : UpdateState()
        data class UpToDate(val currentVersion: String) : UpdateState()
        data class UpdateAvailable(val release: ReleaseInfo) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    fun checkNow(context: Context) {
        val appContext = context.applicationContext
        scope.launch { check(appContext) }
    }

    private fun currentVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (_: Exception) {
        "?"
    }

    private suspend fun check(context: Context) {
        _state.value = UpdateState.Checking
        runCatching {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                connectTimeout = 10_000
                readTimeout = 15_000
            }
            val body = try {
                val code = connection.responseCode
                val payload = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("HTTP $code")
                payload
            } finally {
                connection.disconnect()
            }

            val json = JSONObject(body)
            val tag = json.getString("tag_name").trimStart('v')
            val name = json.optString("name", tag)
            val releaseBody = json.optString("body", "")
            val htmlUrl = json.getString("html_url")

            val assets = json.optJSONArray("assets") ?: JSONArray()
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }

            val release = ReleaseInfo(
                tagName = tag,
                name = name,
                body = releaseBody,
                apkUrl = apkUrl,
                htmlUrl = htmlUrl,
                publishedAt = json.optString("published_at")
            )

            val version = currentVersion(context)
            _state.value = if (isNewer(tag, version))
                UpdateState.UpdateAvailable(release)
            else
                UpdateState.UpToDate(version)
        }.onFailure { e ->
            _state.value = UpdateState.Error(e.message ?: "Unknown error")
        }
    }

    private fun isNewer(candidate: String, current: String): Boolean {
        val clean = { v: String -> v.substringBefore("-").substringBefore("_") }
        val cParts = clean(candidate).split(".").mapNotNull { it.toIntOrNull() }
        val oParts = clean(current).split(".").mapNotNull { it.toIntOrNull() }
        if (cParts.isEmpty() || oParts.isEmpty()) return candidate != current
        val len = maxOf(cParts.size, oParts.size)
        for (i in 0 until len) {
            val c = cParts.getOrElse(i) { 0 }
            val o = oParts.getOrElse(i) { 0 }
            if (c > o) return true
            if (c < o) return false
        }
        return false
    }
}
