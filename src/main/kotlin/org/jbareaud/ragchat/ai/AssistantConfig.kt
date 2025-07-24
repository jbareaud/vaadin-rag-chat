package org.jbareaud.ragchat.ai

import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilderFactory
import dev.langchain4j.model.ollama.OllamaModels
import org.jbareaud.ragchat.ai.chroma.ChromaClient
import org.jbareaud.ragchat.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AssistantConfig(private val properties: ConfigProperties) {

    @Bean fun httpClientBuilder() = SpringRestClientBuilderFactory().create()

    @Bean
    fun ollamaModels(props: ConfigProperties) =
        OllamaModels.builder()
            .baseUrl(requireNotNull(props.ollama?.baseUrl))
            .httpClientBuilder(httpClientBuilder())
            .build()

    @Bean
    @ConditionalOnProperty(name = ["rag-chat.chroma.base-url"])
    fun chromaClient(props: ConfigProperties): ChromaClient? =
        props.chroma?.let {
            try {
                ChromaClient(props)
                .also {
                    it.collectionNames()
                }.also {
                    logger().info("Initialized chroma client")
                }
            } catch (err: Throwable) {
                logger().error("Couldn't initialize Chroma client")
                null
            }
        }
}