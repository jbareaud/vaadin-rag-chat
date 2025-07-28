package org.jbareaud.ragchat.ai.chat

import dev.langchain4j.http.client.HttpClientBuilder
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.service.AiServices
import org.jbareaud.ragchat.ai.ConfigProperties
import org.jbareaud.ragchat.ai.provider.AssistantProvider
import org.jbareaud.ragchat.ai.provider.streamingChatModel
import org.springframework.stereotype.Service


@Service
class ChatAssistantProvider(
    private val props: ConfigProperties,
    private val httpClientBuilder: HttpClientBuilder,
): AssistantProvider<ChatParameters, ChatAssistant> {

    override fun instantiateAssistant(parameters: ChatParameters): ChatAssistant {

        val streamingChatModel = streamingChatModel(
            chatModelName = parameters.chatModelName,
            httpClientBuilder = httpClientBuilder,
            props = props,
        )

        return AiServices.builder(ChatAssistant::class.java)
            .streamingChatModel(streamingChatModel)
            .chatMemoryProvider { MessageWindowChatMemory.withMaxMessages(props.memoryProviderMaxMessages) }
            .build()
    }
}

data class ChatParameters(
    val chatModelName: String,
)