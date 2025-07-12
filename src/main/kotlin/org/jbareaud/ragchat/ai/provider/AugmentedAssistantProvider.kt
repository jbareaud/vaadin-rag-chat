package org.jbareaud.ragchat.ai.provider

import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.http.client.HttpClientBuilder
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.model.scoring.ScoringModel
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.jbareaud.ragchat.ai.ConfigProperties
import org.jbareaud.ragchat.ai.AssistantType
import org.jbareaud.ragchat.logger
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.temporal.ChronoUnit

@Service
class AugmentedAssistantProvider(
    props: ConfigProperties,
    httpClientBuilder: HttpClientBuilder,
    private val scoringModel: ScoringModel? = null,
): SimpleAssistantProvider(props, httpClientBuilder) {

    override fun type() = AssistantType.AUGMENTED

    override fun instantiateAssistant(
        chatModelName: String,
        embeddingModelName: String?,
        useReranker: Boolean,
        docsLocation: String
    ): RagAssistant {

        val streamingChatModel = streamingChatModel(chatModelName)

        val embeddingModel = embeddingModel(embeddingModelName)

        val embeddingStore = InMemoryEmbeddingStore<TextSegment>()

        val ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(documentSplitter())
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build()

        val docs = FileSystemDocumentLoader.loadDocuments(docsLocation)

        try {
            ingestor.ingest(docs)
        } catch (err: Exception) {
            val message = "Error during document ingestion"
            logger().error("message: $err")
            throw RuntimeException(message, err)
        }

        val contentRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(props.contentRetrieverMaxResults)
            .build()

        val retrievalAugmentor = DefaultRetrievalAugmentor.builder()
            .contentRetriever(contentRetriever)
            .apply {
                if (useReranker) reRankingContentAggregator()?.let { contentAggregator(it) }
            }
            .build()

        return AiServices.builder(RagAssistant::class.java)
            .streamingChatModel(streamingChatModel)
            .chatMemoryProvider { MessageWindowChatMemory.withMaxMessages(props.memoryProviderMaxMessages) }
            .retrievalAugmentor(retrievalAugmentor)
            .build()

    }

    private fun reRankingContentAggregator(): ReRankingContentAggregator? {
        return scoringModel?.let {
            ReRankingContentAggregator.builder()
                .scoringModel(it)
                .minScore(requireNotNull(props.scoringMinScore))
                .build()
        }
    }

    protected fun documentSplitter(): DocumentSplitter =
        DocumentSplitters.recursive(props.splitterMaxChars, props.splitterOverlapChars)

    protected fun embeddingModel(embeddingModelName: String?) =
        embeddingModelName?.let {
            OllamaEmbeddingModel.builder()
                .baseUrl(props.chatOllamaBaseUrl)
                .modelName(it)
                .timeout(Duration.of(5, ChronoUnit.MINUTES))
                .httpClientBuilder(httpClientBuilder)
                .build().also {
                    logger().info("Initializing ollama embedding model $embeddingModelName")
                }
        }
            ?: BgeSmallEnV15QuantizedEmbeddingModel().also {
                logger().info("using default embedding model")
            }
}