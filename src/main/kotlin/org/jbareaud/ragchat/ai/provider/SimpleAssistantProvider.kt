package org.jbareaud.ragchat.ai.provider

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.http.client.HttpClientBuilder
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.jbareaud.ragchat.ai.ConfigProperties
import org.jbareaud.ragchat.ai.AssistantType
import org.jbareaud.ragchat.logger
import org.springframework.stereotype.Service

@Service
class SimpleAssistantProvider(
    protected val props: ConfigProperties,
    protected val httpClientBuilder: HttpClientBuilder,
): AssistantProvider {

    override fun type() = AssistantType.SIMPLE

    override fun instantiateAssistant(
        chatModelName: String,
        embeddingModelName: String?,
        useReranker: Boolean,
        docsLocation: String
    ): RagAssistant {
        if (embeddingModelName != null) {
            logger().info("Usage of embedding model $embeddingModelName will be ignored for a simple chat")
        }
        if (useReranker) {
            logger().info("Usage of reranker will be ignored for a simple chat (default used by langchain4j should be bge-small-en-v15)")
        }
        val embeddingStore = InMemoryEmbeddingStore<TextSegment>()
        val contentRetriever = EmbeddingStoreContentRetriever.from(embeddingStore)
        val docs = FileSystemDocumentLoader.loadDocuments(docsLocation)
        EmbeddingStoreIngestor.ingest(docs, embeddingStore)
        return AiServices.builder(RagAssistant::class.java)
            .streamingChatModel(streamingChatModel(chatModelName))
            .chatMemoryProvider { MessageWindowChatMemory.withMaxMessages(props.memoryProviderMaxMessages) }
            .contentRetriever(contentRetriever)
            .build()
    }

    protected fun streamingChatModel(chatModelName: String): StreamingChatModel =
        OllamaStreamingChatModel.builder()
            .baseUrl(props.chatOllamaBaseUrl)
            .modelName(chatModelName)
            .temperature(props.chatTemperature)
            .httpClientBuilder(httpClientBuilder)
            .build().also {
                logger().info("Initializing Ollama chat with $chatModelName")
            }
}