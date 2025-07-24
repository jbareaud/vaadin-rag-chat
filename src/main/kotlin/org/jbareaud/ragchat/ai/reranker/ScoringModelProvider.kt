package org.jbareaud.ragchat.ai.reranker

import dev.langchain4j.model.scoring.ScoringModel
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel
import org.jbareaud.ragchat.ai.ConfigProperties
import org.jbareaud.ragchat.logger
import org.springframework.stereotype.Service

@Service
class ScoringModelProvider(props: ConfigProperties) {

    private val models: Map<String, Lazy<ScoringModel>> = props.scoring?.list?.associate { item ->
        item.name.trim() to lazy {
            when(item.type) {
                ScoringType.ONNX -> OnnxScoringModel(requireNotNull(item.pathToModel), requireNotNull(item.pathToTokenizer))
                ScoringType.LLM ->  OllamaScoringModel.builder()
                    .baseUrl(requireNotNull(props.ollama?.baseUrl))
                    .modelName(requireNotNull(item.modelName))
                    .temperature(0.1)
                    .build()
            }.also {
                logger().info("Initialization of scoring model ${item.name} for first usage")
            }
        }
    } ?: emptyMap()

    fun availables() = models.keys.toList()

    fun provide(name: String) = models[name]?.value

}

