package org.jbareaud.ragchat.ai

import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfig {

    @Value("\${langchain4j.chat-model.base-url}") private lateinit var ollamaUrl: String
    @Value("\${langchain4j.chat-model.model-name}") private lateinit var ollamaModel: String
    @Value("\${langchain4j.chat-model.temperature}") private lateinit var temperature: String

    @Bean
    fun ollamaChatModel(): StreamingChatModel =
        OllamaStreamingChatModel.builder()
            .baseUrl(ollamaUrl)
            .modelName(ollamaModel)
            .temperature(temperature.toDouble())
            //.logRequests(true)
            //.logResponses(true)
            .build()

}