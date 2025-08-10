package org.jbareaud.ragchat.ui

import org.jbareaud.ragchat.ai.ChatType

object ChatSessionStore {
    private val chatSessions = mutableMapOf<String, ChatSession>()

    fun save(session: ChatSession) {
        chatSessions[session.chatId] = session
    }

    fun get(chatId: String): ChatSession? = chatSessions[chatId]
}

data class ChatSession(
    val chatId: String,
    val title: String,
    val chatType: ChatType,
    val chatModel: String,
    val settings: Map<String, String> = mapOf(),
)