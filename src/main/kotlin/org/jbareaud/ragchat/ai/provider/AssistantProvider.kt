package org.jbareaud.ragchat.ai.provider

import org.jbareaud.ragchat.ai.AssistantType

interface AssistantProvider {

    fun type(): AssistantType

    fun instantiateAssistant(
        chatModelName:String,
        embeddingModelName:String?,
        useReranker: Boolean,
        docsLocation: String
    ): RagAssistant
}