package org.jbareaud.ragchat.ai.provider

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["chat.service.provider"], havingValue = "SIMPLE")
class SimpleAssistantProvider(
    private val chatModel: StreamingChatModel,
    @Value("\${chat.service.memory-provider.max-messages}") private val maxMessages: String,
): AssistantProvider {

    private val instantiate: (String) -> RagAssistant = { docsLocation ->

        val embeddingStore = InMemoryEmbeddingStore<TextSegment>()

        val contentRetriever = EmbeddingStoreContentRetriever.from(embeddingStore)

        val docs = FileSystemDocumentLoader.loadDocuments(docsLocation)
        EmbeddingStoreIngestor.ingest(docs, embeddingStore)

        AiServices.builder(RagAssistant::class.java)
            .streamingChatModel(chatModel)
            .chatMemoryProvider { MessageWindowChatMemory.withMaxMessages(maxMessages.toInt()) }
            .contentRetriever(contentRetriever)
            .build()
    }

    override fun newAssistant(docsLocation: String): RagAssistant {
        return instantiate(docsLocation)
    }
}