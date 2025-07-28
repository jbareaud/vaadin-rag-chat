package org.jbareaud.ragchat.ai.provider

import dev.langchain4j.http.client.HttpClientBuilder
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import org.jbareaud.ragchat.ai.ConfigProperties

fun streamingChatModel(
    props: ConfigProperties,
    httpClientBuilder: HttpClientBuilder,
    chatModelName: String
): StreamingChatModel =
    OllamaStreamingChatModel.builder()
        .baseUrl(requireNotNull(props.ollama?.baseUrl))
        .modelName(chatModelName)
        .temperature(requireNotNull(props.ollama?.temperature))
        .topK(requireNotNull(props.ollama?.topK))
        .httpClientBuilder(httpClientBuilder)
        .logRequests(true)
        .build()