package org.jbareaud.ragchat.ai.provider

import dev.langchain4j.service.TokenStream

interface Assistant {
    fun chat(memoryId: String, message: String): TokenStream
}