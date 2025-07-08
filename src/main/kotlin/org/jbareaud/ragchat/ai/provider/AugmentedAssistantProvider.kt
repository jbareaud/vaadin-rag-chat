package org.jbareaud.ragchat.ai.provider

import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["chat.service.provider"], havingValue = "AUGMENTED")
class AugmentedAssistantProvider(
    private val chatModel: StreamingChatModel,
    @Value("\${chat.service.content-retriever.max-results}") private val maxResults: String,
    @Value("\${chat.service.memory-provider.max-messages}") private val maxMessages: String,
): AssistantProvider {

    private val instantiate: (String) -> RagAssistant =  { docsLocation ->

        val embeddingModel = BgeSmallEnV15QuantizedEmbeddingModel()

        val embeddingStore = InMemoryEmbeddingStore<TextSegment>()

        val ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(documentSplitter())
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build()

        val docs = FileSystemDocumentLoader.loadDocuments(docsLocation)
        ingestor.ingest(docs);

        val contentRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(maxResults.toInt())
            .build()

        val retrievalAugmentor = DefaultRetrievalAugmentor.builder()
            .contentRetriever(contentRetriever)
            .apply {
                reRankingContentAggregator()?.let { contentAggregator(it) }
            }
            .build()

        AiServices.builder(RagAssistant::class.java)
            .streamingChatModel(chatModel)
            .chatMemoryProvider { MessageWindowChatMemory.withMaxMessages(maxMessages.toInt()) }
            .retrievalAugmentor(retrievalAugmentor)
            .build()
    }

    override fun newAssistant(docsLocation: String): RagAssistant {
        return instantiate(docsLocation)
    }

    private fun reRankingContentAggregator(): ReRankingContentAggregator? {
        // Need a valid api key to use scoring model
        /*
        val scoringModel = CohereScoringModel.builder()
            .apiKey(System.getenv("COHERE_API_KEY"))
            .modelName("rerank-multilingual-v3.0")
            .build();

        return ReRankingContentAggregator.builder()
                    .scoringModel(scoringModel)
                    .minScore(0.8)
                    .build()
         */
        return null
    }

    protected fun documentSplitter(): DocumentSplitter =
        DocumentSplitters.recursive(300, 100)
}