package org.jbareaud.ragchat.ai.provider

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.http.client.HttpClientBuilder
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.jbareaud.ragchat.ai.ConfigProperties
import org.jbareaud.ragchat.ai.AssistantType
import org.jbareaud.ragchat.ai.chroma.ChromaClient
import org.jbareaud.ragchat.logger
import org.springframework.stereotype.Service

@Service
class SimpleAssistantProvider(
    protected val props: ConfigProperties,
    protected val httpClientBuilder: HttpClientBuilder,
    private val client: ChromaClient? = null,
): AssistantProvider {

    override fun type() = AssistantType.SIMPLE

    override fun instantiateAssistant(
        chatModelName: String,
        collectionName: String?,
        createKnowledgeBase: Boolean,
        embeddingModelName: String?,
        rerankerModelName: String?,
        docsLocation: String?
    ): RagAssistant {
        checkParameters(docsLocation, embeddingModelName, rerankerModelName)
        val embeddingStore = embeddingStore(collectionName)
        val contentRetriever = EmbeddingStoreContentRetriever.from(embeddingStore)
        if (createKnowledgeBase) {
            val docs = FileSystemDocumentLoader.loadDocuments(docsLocation)
            EmbeddingStoreIngestor.ingest(docs, embeddingStore)
        }
        return AiServices.builder(RagAssistant::class.java)
            .streamingChatModel(streamingChatModel(chatModelName))
            .chatMemoryProvider { MessageWindowChatMemory.withMaxMessages(props.memoryProviderMaxMessages) }
            .contentRetriever(contentRetriever)
            .build()
    }

    private fun checkParameters(
        docsLocation: String?,
        embeddingModelName: String?,
        rerankerModelName: String?
    ) {
        if (embeddingModelName != null) {
            logger().warn("Usage of embedding model $embeddingModelName will be ignored for a simple chat")
        }
        if (rerankerModelName != null) {
            logger().warn("Usage of reranker will be ignored for a simple chat (default used by langchain4j should be bge-small-en-v15)")
        }
        if (docsLocation == null) {
            val message = "Need a document location for simple chat"
            logger().error(message)
            throw RuntimeException(message)
        }
    }

    protected fun streamingChatModel(chatModelName: String): StreamingChatModel =
        OllamaStreamingChatModel.builder()
            .baseUrl(requireNotNull(props.ollama?.baseUrl))
            .modelName(chatModelName)
            .temperature(requireNotNull(props.ollama?.temperature))
            .topK(requireNotNull(props.ollama?.topK))
            .httpClientBuilder(httpClientBuilder)
            .build().also {
                logger().info("Initializing Ollama chat with $chatModelName")
            }

    protected fun embeddingStore(collectionName: String?): EmbeddingStore<TextSegment> {
        val store = props.chroma?.baseUrl?.let { baseUrl ->
            try {
                ChromaEmbeddingStore.builder()
                    .baseUrl(baseUrl)
                    .timeout(props.chroma?.storeTimeout)
                    .collectionName(collectionName)
                    //.logRequests(true)
                    //.logResponses(true)
                    .build().also {
                        logger().info("Creating new ChromaEmbeddingStore for $collectionName")
                    }
            } catch (e: Throwable) {
                logger().error("Couldn't initialize chroma embedding store: $e")
                null
            }
        }

        return store
            ?: InMemoryEmbeddingStore<TextSegment>().also {
                    logger().info("Creating new in-memory embedding store for $collectionName")
            }
    }
}