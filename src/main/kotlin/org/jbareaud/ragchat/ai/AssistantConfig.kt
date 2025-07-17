package org.jbareaud.ragchat.ai

import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilderFactory
import dev.langchain4j.model.ollama.OllamaModels
import dev.langchain4j.model.scoring.ScoringModel
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore
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
            .baseUrl(props.chatOllamaBaseUrl)
            .httpClientBuilder(httpClientBuilder())
            .build()

    @Bean
    @ConditionalOnProperty(name = ["rag-chat.scoring-enabled"], havingValue = "true")
    fun onnxScoringModel(props: ConfigProperties): ScoringModel =
        OnnxScoringModel(props.scoringPathToModel, props.scoringPathToTokenizer).also {
            logger().info("Initialized optional ONNX scoring model from path ${props.scoringPathToModel}")
        }

    @Bean
    @ConditionalOnProperty(name = ["rag-chat.chroma-enabled"], havingValue = "true")
    fun chromaClient(props: ConfigProperties): ChromaClient =
        ChromaClient(props).also {
            logger().info("Initialized chroma client")
        }
}