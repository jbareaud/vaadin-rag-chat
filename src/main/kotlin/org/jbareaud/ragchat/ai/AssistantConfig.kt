package org.jbareaud.ragchat.ai

import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilderFactory
import dev.langchain4j.model.ollama.OllamaModels
import dev.langchain4j.model.scoring.ScoringModel
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel
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
            .build()

    @Bean
    @ConditionalOnProperty(name = ["rag-chat.scoring-enabled"], havingValue = "true")
    fun onnxScoringModel(props: ConfigProperties): ScoringModel =
        if (props.scoringPathToModel != null && props.scoringPathToTokenizer != null)
            OnnxScoringModel(props.scoringPathToModel, props.scoringPathToTokenizer).also {
                logger().info("Initializing optional ONNX scoring model from path ${props.scoringPathToModel}")
            }
        else throw AssistantException("ONNX scoring model enabled but couldn't be instantiated")

}