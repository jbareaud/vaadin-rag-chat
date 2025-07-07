package org.jbareaud.ragchat.ai.provider

import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.model.chat.StreamingChatModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["chat.service.provider"], havingValue = "MEDIAWIKI")
class MediawikiAssistantProvider(
    chatModel: StreamingChatModel,
    @Value("\${chat.service.content-retriever.max-results}") maxResults: String,
    @Value("\${chat.service.memory-provider.max-messages}") maxMessages: String,
): AugmentedAssistantProvider(chatModel, maxResults, maxMessages) {

    override fun documentSplitter(): DocumentSplitter {
        // regex splitter for Mediawiki documents
        return DocumentByRegexSplitter(
            "(?:<page.*?>)(.*?)(?:<\\/page>)",
            "",
            300,
            100,
            DocumentSplitters.recursive(300, 100)
        )
    }
}