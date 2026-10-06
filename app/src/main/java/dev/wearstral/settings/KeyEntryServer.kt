package dev.wearstral.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * One-shot web server that lets the user enter the API key on another device
 * (phone or computer) connected to the same Wi-Fi as the watch.
 *
 * The watch displays the server URL plus a random 6-digit PIN; the PIN must
 * be typed on the web form before the key is accepted. The server binds only
 * to the Wi-Fi interface, accepts a single save, gives up after a few wrong
 * PINs and stops itself after [SESSION_TIMEOUT_MS] without a successful save.
 * The key is handled in memory only and handed to [onKeySubmitted].
 */
class KeyEntryServer(private val onKeySubmitted: (String) -> Unit) {

    sealed class State {
        data object Idle : State()
        data class Running(val url: String, val pin: String) : State()
        data object Done : State()
        data class Failed(val reason: Reason) : State()

        enum class Reason { NoWifi, Error }
    }

    private enum class Outcome { SAVED, BAD_PIN, IGNORE }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (serverSocket != null) return
        scope.launch {
            val address = wifiAddress()
            if (address == null) {
                _state.value = State.Failed(State.Reason.NoWifi)
                return@launch
            }
            try {
                val socket = ServerSocket(0, BACKLOG, address)
                serverSocket = socket
                socket.soTimeout = SESSION_TIMEOUT_MS.toInt()
                val url = "http://${socket.inetAddress.hostAddress}:${socket.localPort}"
                val pin = (100_000..999_999).random().toString()
                _state.value = State.Running(url, pin)
                serve(socket, pin)
            } catch (e: Exception) {
                if (_state.value !is State.Done) {
                    _state.value = State.Failed(State.Reason.Error)
                }
            } finally {
                runCatching { serverSocket?.close() }
                serverSocket = null
                if (_state.value is State.Running) _state.value = State.Idle
            }
        }
    }

    /** Stops the session; safe to call at any time, including from onDispose. */
    fun stop() {
        runCatching { serverSocket?.close() }
    }

    private fun serve(socket: ServerSocket, pin: String) {
        var badPins = 0
        while (true) {
            val client = try {
                socket.accept()
            } catch (e: Exception) {
                break // session timed out or was stopped
            }
            when (handle(client, pin)) {
                Outcome.SAVED -> {
                    _state.value = State.Done
                    return
                }
                Outcome.BAD_PIN -> {
                    badPins++
                    if (badPins >= MAX_BAD_PINS) return
                }
                Outcome.IGNORE -> Unit
            }
        }
    }

    private fun handle(client: Socket, pin: String): Outcome = try {
        client.soTimeout = CLIENT_TIMEOUT_MS.toInt()
        val reader = BufferedReader(
            InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
        )
        val method = reader.readLine()?.substringBefore(' ') ?: return Outcome.IGNORE
        var contentLength = 0
        while (true) {
            val header = reader.readLine() ?: break
            if (header.isEmpty()) break
            if (header.lowercase().startsWith("content-length:")) {
                contentLength = header.substringAfter(':').trim().toIntOrNull()
                    ?.coerceAtMost(MAX_BODY_BYTES) ?: 0
            }
        }
        if (method == "POST") {
            val body = buildString {
                repeat(contentLength) {
                    val c = reader.read()
                    if (c < 0) return@repeat
                    append(c.toChar())
                }
            }
            val submittedPin = param(body, "pin")
            val key = param(body, "key")?.trim() ?: ""
            when {
                submittedPin != pin -> {
                    respond(client, "403 Forbidden", page("Wrong PIN", "Check the PIN shown on the watch and try again."))
                    Outcome.BAD_PIN
                }
                key.length < MIN_KEY_LENGTH -> {
                    respond(client, "400 Bad Request", page("Key too short", "That does not look like a Mistral API key."))
                    Outcome.IGNORE
                }
                else -> {
                    onKeySubmitted(key)
                    respond(client, "200 OK", page("Key saved", "You can close this page and pick up your watch."))
                    Outcome.SAVED
                }
            }
        } else {
            respond(client, "200 OK", FORM_PAGE)
            Outcome.IGNORE
        }
    } catch (e: Exception) {
        Outcome.IGNORE
    } finally {
        runCatching { client.close() }
    }

    private fun respond(client: Socket, status: String, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        client.getOutputStream().use { out ->
            out.write(
                (
                    "HTTP/1.1 $status\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Cache-Control: no-store\r\n" +
                        "Connection: close\r\n\r\n"
                    ).toByteArray(StandardCharsets.US_ASCII)
            )
            out.write(bytes)
            out.flush()
        }
    }

    private fun param(body: String, name: String): String? =
        body.split('&').firstOrNull { it.substringBefore('=') == name }
            ?.substringAfter('=', "")
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }

    private fun wifiAddress(): Inet4Address? = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { networkInterface -> networkInterface.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
    }.getOrNull()

    private val STYLE = """
        <style>
        body{font-family:system-ui,sans-serif;background:#0e0e10;color:#fff;max-width:480px;margin:40px auto;padding:0 16px}
        h1{font-size:20px}
        input,button{width:100%;box-sizing:border-box;font-size:16px;padding:12px;border-radius:10px;border:1px solid #44474b;background:#1e1e22;color:#fff;margin:6px 0}
        button{background:#ff8a65;color:#000;font-weight:700;border:none}
        </style>
    """.trimIndent()

    private fun page(title: String, text: String) = """
        <!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Wearstral</title>$STYLE</head>
        <body><h1>$title</h1><p>$text</p></body></html>
    """.trimIndent()

    private val FORM_PAGE = """
        <!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Wearstral key</title>$STYLE</head>
        <body><h1>Wearstral — API key</h1>
        <form method="post" action="/">
        <input name="pin" inputmode="numeric" placeholder="6-digit PIN shown on the watch" required>
        <input name="key" placeholder="Mistral API key" required>
        <button type="submit">Save on the watch</button></form></body></html>
    """.trimIndent()

    private companion object {
        const val BACKLOG = 5
        const val SESSION_TIMEOUT_MS = 5 * 60_000L
        const val CLIENT_TIMEOUT_MS = 10_000L
        const val MAX_BAD_PINS = 5
        const val MIN_KEY_LENGTH = 20
        const val MAX_BODY_BYTES = 8 * 1024
    }
}
