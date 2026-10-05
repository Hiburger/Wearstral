package dev.wearstral.chat

enum class ChatError {
    InvalidKey,
    RateLimited,
    Server,
    Network,
    Unknown
}

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val error: ChatError? = null
)
