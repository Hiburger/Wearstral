package dev.wearstral.chat

enum class Role(val wire: String) {
    User("user"),
    Assistant("assistant")
}

data class ChatMessage(
    val role: Role,
    val content: String
)
