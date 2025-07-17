package org.jbareaud.ragchat.ai

import dev.langchain4j.model.ollama.OllamaModel
import dev.langchain4j.model.ollama.OllamaModels
import dev.langchain4j.model.scoring.ScoringModel
import dev.langchain4j.service.TokenStream
import org.jbareaud.ragchat.ai.provider.AssistantProvider
import org.jbareaud.ragchat.ai.provider.RagAssistant
import org.jbareaud.ragchat.ai.chroma.ChromaClient
import org.jbareaud.ragchat.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks


@Service
class AssistantChatService(
    private val providers: List<AssistantProvider>,
    private val ollamaModels: OllamaModels,
    private val scoringModel: ScoringModel?,
    private val chromaClient: ChromaClient?,
    @Value("\${rag-chat.embedding-families}") private val embeddingFamilies:List<String>,
    @Value("\${rag-chat.chat-families}") private val chatFamilies:List<String>,
    @Value("\${rag-chat.default-chat-selection}") private val defaultChatSelection:List<String>,
) {

    private var assistant: RagAssistant = object : RagAssistant {
        override fun chat(memoryId: String, message: String): TokenStream {
            throw RuntimeException("assistant not initialized yet")
        }
    }

    private val listModels by lazy {
        ollamaModels.availableModels().content()
    }

    fun types() = providers.map(AssistantProvider::type).sorted()

    fun embeddings() = listModels.toNameList(embeddingFamilies)

    fun chatModels() = listModels.toNameList(chatFamilies)

    fun hasReranker() = scoringModel != null

    fun defaultChatModel() =
        defaultChatSelection
            .intersect(chatModels().toSet())
            .firstOrNull()

    fun dataStores() = chromaClient?.collectionNames().orEmpty()

    fun newAssistant(
        type: AssistantType,
        chatModelName: String,
        collectionName: String?,
        createKnowledgeBase: Boolean,
        embeddingModelName: String?,
        useReranker: Boolean,
        docsLocation: String?
    ) {
        checkAssistantType(type)
        logger().info("Initializing new assistant of type $type")
        assistant = providers
            .first { it.type() == type }
            .instantiateAssistant(
                chatModelName = chatModelName,
                collectionName = collectionName,
                createKnowledgeBase = createKnowledgeBase,
                embeddingModelName = embeddingModelName,
                useReranker = useReranker,
                docsLocation = docsLocation
            )
        logger().info("Finished initializing new assistant")
    }

    private fun checkAssistantType(type: AssistantType) {
        if (type !in types()) {
            val message = "$type chat type requested is not available"
            logger().error(message)
            throw AssistantException(message)
        }
    }

    fun streamNewMessage(chatId: String, userMessage: String): Flux<String> {
        val sink = Sinks.many().unicast().onBackpressureBuffer<String>()
        assistant.chat(chatId, userMessage)
            .onPartialResponse { partial ->
                sink.tryEmitNext(partial)
            }
            .onCompleteResponse { _ ->
                sink.tryEmitComplete()
            }
            .onError { err ->
                logger().error("Error during streaming of message : $err")
                sink.tryEmitError(err)
            }
            .start()
        return sink.asFlux()
    }
}

private fun List<OllamaModel>.toNameList(familyList: List<String>) =
    filter { it.details.family in familyList }
        .map { it.name }
        .sorted()
