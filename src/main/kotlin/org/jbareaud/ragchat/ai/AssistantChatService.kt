package org.jbareaud.ragchat.ai

import dev.langchain4j.model.ollama.OllamaModels
import dev.langchain4j.model.scoring.ScoringModel
import org.jbareaud.ragchat.ai.provider.AssistantProvider
import org.jbareaud.ragchat.ai.provider.RagAssistant
import org.jbareaud.ragchat.logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks


@Service
class AssistantChatService(
    private val providers: List<AssistantProvider>,
    private val ollamaModels: OllamaModels,
    private val scoringModel: ScoringModel? = null
) {

    companion object {
        private val embeddingModelFamilyNames = setOf("bert", "nomic-bert") // TODO exporter
        private val chatModelFamilyNames = setOf("llama", "qwen2", "qwen3", "gemma3")  // TODO exporter
    }

    private var assistant: RagAssistant? = null

    private val listEmbeddings by lazy {
        ollamaModels.availableModels().content()
    }

    fun available() = providers.map(AssistantProvider::type)

    fun embeddings() = listEmbeddings
        .filter { it.details.family in embeddingModelFamilyNames }
        .map { it.name }

    fun models() = listEmbeddings
        .filter { it.details.family in chatModelFamilyNames }
        .map { it.name }

    fun hasReranker() = scoringModel != null

    fun newAssistant(
        type:AssistantType,
        chatModelName:String,
        embeddingModelName:String?,
        useReranker: Boolean,
        docsLocation: String
    ) {
        // TODO use embeddingModelName
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

