package dev.wearstral.chat

data class Conversation(
    val id: Long,
    val updatedAt: Long,
    val messages: List<ChatMessage>,
    val pinned: Boolean = false
) {
    val title: String
        get() = (messages.firstOrNull { it.role == Role.User } ?: messages.firstOrNull())
            ?.content.orEmpty()
}
