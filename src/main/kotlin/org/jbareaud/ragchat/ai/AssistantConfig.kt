package org.jbareaud.ragchat.ai

import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilderFactory
import dev.langchain4j.model.ollama.OllamaModels
import org.jbareaud.ragchat.ai.reranker.OllamaScoringModel
import dev.langchain4j.model.scoring.ScoringModel
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel
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
    @ConditionalOnProperty(name = ["rag-chat.scoring-type"], havingValue = "ONNX")
    fun onnxScoringModel(props: ConfigProperties): ScoringModel =
        OnnxScoringModel(props.onnxScoringPathToModel, props.onnxScoringPathToTokenizer).also {
            logger().info("Initialized optional ONNX scoring model from path ${props.onnxScoringPathToModel}")
        }

    @Bean
    @ConditionalOnProperty(name = ["rag-chat.scoring-type"], havingValue = "LLM")
    fun llmScoringModel(props: ConfigProperties): ScoringModel =
        OllamaScoringModel.builder()
            .baseUrl(props.chatOllamaBaseUrl)
            .modelName(props.llmScoringModelName)
            .temperature(0.1)
            .build().also {
                logger().info("Initialized optional LLM scoring model with model ${props.llmScoringModelName}")
            }

    @Bean
    @ConditionalOnProperty(name = ["rag-chat.chroma-enabled"], havingValue = "true")
    fun chromaClient(props: ConfigProperties): ChromaClient =
        ChromaClient(props).also {
            logger().info("Initialized chroma client")
        }
}