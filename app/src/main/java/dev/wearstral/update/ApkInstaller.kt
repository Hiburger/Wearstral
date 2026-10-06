package dev.wearstral.update

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ApkInstaller {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val UPDATE_APK_NAME = "Wearstral-update.apk"

    /**
     * Deletes an update APK left over by a previous install. Called on app
     * start: by then the system installer is done with the file, so the
     * roughly 40MB can be reclaimed.
     */
    fun cleanup(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: appContext.filesDir
            runCatching { File(dir, UPDATE_APK_NAME).delete() }
        }
    }

    /**
     * Downloads the APK from [url], streaming it in chunks and calling
     * [onProgress] with a 0f-1f fraction as bytes arrive.
     * On completion fires an install intent via FileProvider.
     */
    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val file = File(dir, UPDATE_APK_NAME)

            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 30_000
                connection.readTimeout = 120_000
                val code = connection.responseCode
                if (code !in 200..299) error("HTTP $code")
                val contentLength = connection.contentLengthLong // -1 if unknown

                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var totalRead = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            totalRead += read
                            if (contentLength > 0) {
                                onProgress(totalRead.toFloat() / contentLength.toFloat())
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            onProgress(1f)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val install = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(install)

            file
        }
    }
}
