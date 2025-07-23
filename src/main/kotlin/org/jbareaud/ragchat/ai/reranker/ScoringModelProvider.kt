package org.jbareaud.ragchat.ai.reranker

import dev.langchain4j.model.scoring.ScoringModel
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel
import org.jbareaud.ragchat.ai.ConfigProperties
import org.springframework.stereotype.Service

@Service
class ScoringModelProvider(props: ConfigProperties) {

    private val models = props.scoring?.list?.associate {
        it.name to when(it.type) {
            ScoringType.ONNX -> OnnxScoringModelDefinition(it.name, requireNotNull(it.pathToModel), requireNotNull(it.pathToTokenizer))
            ScoringType.LLM -> LlmScoringModelDefinition(it.name, requireNotNull(it.modelName), props)
        }
    } ?: emptyMap()

    fun availables() = models.keys.toList()

    fun provide(name: String? = "mxbai-rerank-xsmall-v1"): ScoringModel? {
        return models[name]?.scoringModel
    }
}

interface ScoringModelDefinition {
    val name: String
    val scoringModel: ScoringModel
}

class OnnxScoringModelDefinition(
    override val name: String,
    pathToModel: String? = null,
    pathToTokenizer: String? = null,
) : ScoringModelDefinition {
    override val scoringModel: ScoringModel by lazy {
        OnnxScoringModel(pathToModel, pathToTokenizer)
    }
}

class LlmScoringModelDefinition(
    override val name: String,
    modelName: String,
    props: ConfigProperties,
): ScoringModelDefinition {
    override val scoringModel: ScoringModel by lazy {
        OllamaScoringModel.builder()
            .baseUrl(requireNotNull(props.ollama?.baseUrl))
            .modelName(modelName)
            .temperature(0.1)
            .build()
    }
}
