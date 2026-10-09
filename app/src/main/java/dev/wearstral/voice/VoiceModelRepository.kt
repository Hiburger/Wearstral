package dev.wearstral.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

data class VoiceLanguage(
    val id: String,
    val name: String,
    val url: String
)

/**
 * The catalog of downloadable languages, the download/unpack pipeline (zip from alphacephei.com into filesDir/voice/<id>/),
 * and the downloaded-set scan. The active language choice itself is persisted
 * by dev.wearstral.settings.SettingsRepository
 */

class VoiceModelRepository(private val context: Context) {

    val languages: List<VoiceLanguage> = listOf(
        VoiceLanguage("en-us", "English", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"),
        VoiceLanguage("fr", "Français", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip"),
        VoiceLanguage("de", "Deutsch", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip"),
        VoiceLanguage("es", "Español", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"),
        VoiceLanguage("it", "Italiano", "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip"),
        VoiceLanguage("pt", "Português", "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip"),
        VoiceLanguage("nl", "Nederlands", "https://alphacephei.com/vosk/models/vosk-model-small-nl-0.22.zip"),
        VoiceLanguage("ru", "Русский", "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip"),
        VoiceLanguage("cn", "中文", "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip")
    )

    fun languageById(id: String): VoiceLanguage? = languages.firstOrNull { it.id == id }

    // Removes an unpacked model directory
    fun delete(languageId: String) {
        File(voiceDir(), languageId).deleteRecursively()
    }

    private fun voiceDir() = File(context.filesDir, "voice").apply { mkdirs() }

    fun modelDir(languageId: String): File? =
        File(voiceDir(), languageId).takeIf { File(it, "conf/model.conf").exists() }

    fun downloadedLanguageIds(): Set<String> =
        voiceDir().listFiles()
            ?.filter { it.isDirectory && File(it, "conf/model.conf").exists() }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()

    /**
     * Downloads the model zip for a chosen language and unpacks it, reporting integer
     * progress 0..100 through [onProgress]. Throws on any failure; partial
     * files are cleaned up so a retry starts fresh
     */

    suspend fun download(language: VoiceLanguage, onProgress: (Int) -> Unit) =
        withContext(Dispatchers.IO) {
            val tmpZip = File(voiceDir(), "${language.id}.zip.tmp")
            val targetDir = File(voiceDir(), language.id)
            try {
                targetDir.deleteRecursively()
                val connection = URL(language.url).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                // without these a dead connection hangs forever with the row stuck at 0%
                connection.connectTimeout = 10_000
                connection.readTimeout = 30_000
                connection.connect()
                if (connection.responseCode !in 200..299) {
                    throw IOException("HTTP ${connection.responseCode}")
                }
                val total = connection.contentLengthLong
                var received = 0L
                var lastReported = -1
                connection.inputStream.use { input ->
                    tmpZip.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            received += read
                            if (total > 0) {
                                val percent = ((received * 100) / total).toInt().coerceIn(0, 99)
                                if (percent != lastReported) {
                                    lastReported = percent
                                    onProgress(percent)
                                }
                            }
                        }
                    }
                }
                onProgress(99)
                unpack(tmpZip, targetDir)
                if (File(targetDir, "conf/model.conf").exists()) {
                    onProgress(100)
                } else {
                    throw IOException("Unpacked model is incomplete")
                }
            } finally {
                tmpZip.delete()
            }
        }

    private fun unpack(zip: File, targetDir: File) {
        try {
            targetDir.mkdirs()
            ZipInputStream(zip.inputStream().buffered()).use { stream ->
                while (true) {
                    val entry = stream.nextEntry ?: break
                    if (entry.isDirectory) continue
                    // entries live under a single top-level vosk-model-* folder
                    val relative = entry.name.substringAfter('/', "")
                    if (relative.isEmpty()) continue
                    val out = File(targetDir, relative)
                    if (!out.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        throw IOException("Zip entry escapes target dir: ${entry.name}")
                    }
                    out.parentFile?.mkdirs()
                    out.outputStream().use { stream.copyTo(it) }
                    stream.closeEntry()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Model unpack failed: ${e.message?.take(120)}")
            targetDir.deleteRecursively()
            throw IOException("Unpack failed", e)
        }
    }

    private companion object {
        const val TAG = "WearstralVoice"
    }
}
