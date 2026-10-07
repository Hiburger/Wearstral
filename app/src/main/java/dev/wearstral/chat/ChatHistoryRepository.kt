package dev.wearstral.chat

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Chat history persisted to a single JSON file in filesDir. The StateFlow is the
 * single source of truth; writes are fire-and-forget (a watch app can lose at
 * most the very latest exchange on a crash ...which is acceptable no?)
 */

class ChatHistoryRepository(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)
    private val backupFile = File(file.parentFile, "$FILE_NAME.bak")
    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    private val _history = MutableStateFlow<List<Conversation>>(emptyList())
    val history: StateFlow<List<Conversation>> = _history.asStateFlow()

    private val initialLoad: Job = scope.launch { _history.value = load() }
    private var mainFileNeedsRepair = false

    fun upsert(conversation: Conversation) {
        update { current ->
            (current.filterNot { it.id == conversation.id } + conversation)
        }
    }

    fun delete(conversationId: Long) {
        update { current -> current.filterNot { it.id == conversationId } }
    }

    fun clear() {
        update { _ -> emptyList() }
    }

    fun setPinned(conversationId: Long, pinned: Boolean) {
        update { current ->
            current.map { if (it.id == conversationId) it.copy(pinned = pinned) else it }
        }
    }

    private fun update(transform: (List<Conversation>) -> List<Conversation>) {
        scope.launch {
            initialLoad.join()
            _history.value = transform(_history.value).sortedWith(
                compareByDescending<Conversation> { it.pinned }.thenByDescending { it.updatedAt }
            )
            persist(_history.value)
        }
    }

    private fun load(): List<Conversation> {
        loadFrom(file)?.let { return it }
        val backup = loadFrom(backupFile) ?: return emptyList()
        Log.d(TAG, "history file unreadable; recovered ${backup.size} conversations from backup")
        mainFileNeedsRepair = true
        return backup
    }

    // null ONLY when the file exists but cannot be parsed
    private fun loadFrom(source: File): List<Conversation>? = try {
        if (source.exists()) {
            val array = JSONObject(source.readText()).getJSONArray(KEY_CONVERSATIONS)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val messages = obj.getJSONArray(KEY_MESSAGES).let { raw ->
                    (0 until raw.length()).mapNotNull { j ->
                        val message = raw.getJSONObject(j)
                        val role = Role.entries.firstOrNull {
                            it.wire == message.optString(KEY_ROLE)
                        } ?: return@mapNotNull null
                        ChatMessage(role, message.optString(KEY_CONTENT))
                    }
                }
                if (messages.isEmpty()) {
                    null
                } else {
                    Conversation(
                        id = obj.getLong(KEY_ID),
                        updatedAt = obj.getLong(KEY_UPDATED_AT),
                        messages = messages,
                        pinned = obj.optBoolean(KEY_PINNED)
                    )
                }
            }.sortedWith(
                compareByDescending<Conversation> { it.pinned }.thenByDescending { it.updatedAt }
            )
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        Log.d(TAG, "load failed ${e.javaClass.simpleName}: ${e.message?.take(120)}")
        null
    }

    private fun persist(conversations: List<Conversation>) {
        try {
            val array = JSONArray()
            conversations.forEach { conversation ->
                val messages = JSONArray()
                conversation.messages.forEach { message ->
                    messages.put(
                        JSONObject()
                            .put(KEY_ROLE, message.role.wire)
                            .put(KEY_CONTENT, message.content)
                    )
                }
                array.put(
                    JSONObject()
                        .put(KEY_ID, conversation.id)
                        .put(KEY_UPDATED_AT, conversation.updatedAt)
                        .put(KEY_MESSAGES, messages)
                        .put(KEY_PINNED, conversation.pinned)
                )
            }
            val json = JSONObject().put(KEY_CONVERSATIONS, array).toString()
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(json)
            if (file.exists() && !mainFileNeedsRepair) {
                runCatching { file.copyTo(backupFile, overwrite = true) }
            }
            mainFileNeedsRepair = false
            if (!tmp.renameTo(file)) {
                file.writeText(json)
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.d(TAG, "persist failed ${e.javaClass.simpleName}: ${e.message?.take(120)}")
        }
    }

    private companion object {
        const val TAG = "WearstralHistory"
        const val FILE_NAME = "chat_history.json"
        const val KEY_CONVERSATIONS = "conversations"
        const val KEY_ID = "id"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_MESSAGES = "messages"
        const val KEY_ROLE = "role"
        const val KEY_CONTENT = "content"
        const val KEY_PINNED = "pinned"
    }
}
