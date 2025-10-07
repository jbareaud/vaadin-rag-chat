package org.jbareaud.ragchat.ai

import dev.langchain4j.model.ollama.OllamaModel
import dev.langchain4j.model.ollama.OllamaModels
import org.jbareaud.ragchat.ai.chat.ChatAssistantProvider
import org.jbareaud.ragchat.ai.chat.ChatParameters
import org.jbareaud.ragchat.ai.rag.RagAssistantProvider
import org.jbareaud.ragchat.ai.chroma.ChromaClient
import org.jbareaud.ragchat.ai.provider.Assistant
import org.jbareaud.ragchat.ai.rag.RagParameters
import org.jbareaud.ragchat.ai.rag.RagType
import org.jbareaud.ragchat.ai.rag.reranker.ScoringModelProvider
import org.jbareaud.ragchat.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class AssistantChatService(
    private val ragProviders: List<RagAssistantProvider>,
    private val chatProvider: ChatAssistantProvider,
    private val ollamaModels: OllamaModels,
    private val scoringModelProvider: ScoringModelProvider,
    private val chromaClient: ChromaClient?,
    @Value("\${rag-chat.embedding-families}") private val embeddingFamilies:List<String>,
    @Value("\${rag-chat.chat-families}") private val chatFamilies:List<String>,
    @Value("\${rag-chat.default-chat-selection}") private val defaultChatSelection:List<String>,
) {

    private val listModels by lazy {
        ollamaModels.availableModels().content()
    }

    fun ragTypes() = ragProviders.map(RagAssistantProvider::type).sorted()

    fun embeddingModels() = listModels.toNameList(embeddingFamilies)

    fun chatModels() = listModels.toNameList(chatFamilies)

    fun rerankerModels() =  scoringModelProvider.availables()

    fun defaultChatModel() =
        defaultChatSelection
            .intersect(chatModels().toSet())
            .firstOrNull()

    fun dataStores() = chromaClient?.collectionNames().orEmpty()

    fun newAssistant(
        chatType: ChatType,
        ragType: RagType? = null,
        chatModelName: String,
        collectionName: String? = null,
        createKnowledgeBase: Boolean? = null,
        embeddingModelName: String? = null,
        rerankerModelName: String? = null,
        docsLocation: String? = null,
    ): Assistant {
        logger().info("Initializing new assistant of type $ragType")
        return when(chatType) {
            ChatType.SIMPLE -> chatProvider.instantiateAssistant(ChatParameters(chatModelName))
            ChatType.RAG -> {
                checkAssistantType(requireNotNull(ragType))
                ragProviders
                    .first { it.type() == ragType }
                    .instantiateAssistant(
                        RagParameters(
                            chatModelName = chatModelName,
                            collectionName = collectionName,
                            createKnowledgeBase = requireNotNull(createKnowledgeBase),
                            embeddingModelName = embeddingModelName,
                            rerankerModelName = rerankerModelName,
                            docsLocation = docsLocation,
                        )
                    )
            }
        }
    }

    private fun checkAssistantType(type: RagType) {
        if (type !in ragTypes()) {
            val message = "$type chat type requested is not available"
            logger().error(message)
            throw AssistantException(message)
        }
    }
}

private fun List<OllamaModel>.toNameList(familyList: List<String>) =
    filter { it.details.family in familyList }
        .map { it.name }
        .sorted()
