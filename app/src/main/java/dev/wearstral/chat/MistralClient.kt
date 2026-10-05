package dev.wearstral.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MistralApiException(val code: Int, val body: String) :
    Exception("Mistral API error $code")

// mistral small is used as the default model because the API requires a default model. A model picker is planned! ^^
class MistralClient(
    private val endpoint: String = "https://api.mistral.ai/v1/chat/completions",
    private val model: String = "mistral-small-latest"
) {

    suspend fun chat(apiKey: String, messages: List<ChatMessage>): String =
        withContext(Dispatchers.IO) {
            val request = JSONObject()
                .put("model", model)
                .put("messages", JSONArray().apply {
                    messages.forEach { message ->
                        put(
                            JSONObject()
                                .put("role", message.role.wire)
                                .put("content", message.content)
                        )
                    }
                })

            val connection =
                (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 30_000
                }

            try {
                connection.outputStream.use { output ->
                    output.write(request.toString().toByteArray(Charsets.UTF_8))
                }
                val code = connection.responseCode
                val payload = (if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                })?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (code !in 200..299) throw MistralApiException(code, payload)

                JSONObject(payload)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            } finally {
                connection.disconnect()
            }
        }
}
