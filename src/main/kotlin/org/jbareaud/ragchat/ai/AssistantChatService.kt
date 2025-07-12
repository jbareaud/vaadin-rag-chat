package org.jbareaud.ragchat.ai

import dev.langchain4j.model.ollama.OllamaModel
import dev.langchain4j.model.ollama.OllamaModels
import dev.langchain4j.model.scoring.ScoringModel
import org.jbareaud.ragchat.ai.provider.AssistantProvider
import org.jbareaud.ragchat.ai.provider.RagAssistant
import org.jbareaud.ragchat.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks


@Service
class AssistantChatService(
    private val providers: List<AssistantProvider>,
    private val ollamaModels: OllamaModels,
    private val scoringModel: ScoringModel? = null,
    @Value("\${rag-chat.embedding-families}") private val embeddingFamilies:List<String>,
    @Value("\${rag-chat.chat-families}") private val chatFamilies:List<String>,
    @Value("\${rag-chat.default-chat-selection}") private val defaultChatSelection:List<String>,
) {

    private var assistant: RagAssistant? = null

    private val listModels by lazy {
        ollamaModels.availableModels().content()
    }

    fun available() = providers.map(AssistantProvider::type).sorted()

    fun embeddings() = listModels.toNameList(embeddingFamilies)

    fun chatModels() = listModels.toNameList(chatFamilies)

    fun hasReranker() = scoringModel != null

    fun defaultChatModel() =
        defaultChatSelection
            .intersect(chatModels().toSet())
            .firstOrNull()

    fun newAssistant(
        type:AssistantType,
        chatModelName:String,
        embeddingModelName:String?,
        useReranker: Boolean,
        docsLocation: String
    ) {
        if (!available().contains(type)) {
            throw AssistantException("$type chat type requested is not available")
        }
        logger().info("Initializing new assistant of type $type")
        assistant = providers
            .first { it.type() == type }
            .instantiateAssistant(
                chatModelName,
                embeddingModelName,
                useReranker,
                docsLocation
            )
        logger().info("Finished initializing new assistant")
    }

    fun streamNewMessage(chatId: String, userMessage: String): Flux<String> {
        val sink = Sinks.many().unicast().onBackpressureBuffer<String>()
        requireNotNull(assistant).chat(chatId, userMessage)
            .onPartialResponse { partial ->
                sink.tryEmitNext(partial)
            }
            .onCompleteResponse { _ ->
                sink.tryEmitComplete()
            }
            .onError { err ->
                logger().error("$err")
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
