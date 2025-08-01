package org.jbareaud.ragchat.ai.rag

import org.jbareaud.ragchat.ai.provider.AssistantProvider

interface RagAssistantProvider: AssistantProvider<RagParameters, RagAssistant> {

    fun type(): RagType

    override fun instantiateAssistant(parameters: RagParameters): RagAssistant
}

data class RagParameters (
    val chatModelName:String,
    val collectionName: String?,
    val createKnowledgeBase: Boolean,
    val embeddingModelName:String?,
    val rerankerModelName: String?,
    val docsLocation: String?
)