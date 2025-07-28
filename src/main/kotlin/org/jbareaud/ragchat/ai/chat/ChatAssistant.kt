package org.jbareaud.ragchat.ai.chat

import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.UserMessage
import org.jbareaud.ragchat.ai.provider.Assistant

interface ChatAssistant: Assistant {

    @SystemMessage("You are a friendly and helpful assistant.\nIf you do not know the answer, say \"I don't know\".")
    override fun chat(@MemoryId memoryId: String, @UserMessage message: String): TokenStream
}
