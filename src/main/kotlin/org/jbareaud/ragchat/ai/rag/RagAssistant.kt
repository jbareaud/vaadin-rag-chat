package org.jbareaud.ragchat.ai.rag

import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.UserMessage
import org.jbareaud.ragchat.ai.provider.Assistant

interface RagAssistant: Assistant {
    @SystemMessage(
        "You are a friendly and helpful assistant.\nAnswer the questions as accurately as possible using the provided documents.\nIf you do not know the answer, say \"I don't know\"."
    )
    override fun chat(@MemoryId memoryId: String, @UserMessage message: String): TokenStream
}