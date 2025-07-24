package org.jbareaud.ragchat.ai

import org.jbareaud.ragchat.ai.reranker.ScoringType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import java.time.Duration

@ConfigurationProperties(prefix = "rag-chat")
data class ConfigProperties @ConstructorBinding constructor(
    var ollama: OllamaProperties? = null,
    var scoring: ScoringProperties? = null,
    val contentRetrieverMaxResults: Int,
    val memoryProviderMaxMessages: Int,
    var splitter: SplitterProperties? = null,
    var chroma: ChromaProperties? = null,
)

data class ChromaProperties(
    val baseUrl: String?,
    val clientTimeout: Duration?,
    val storeTimeout: Duration?,
)

data class SplitterProperties(
    val maxChars: Int,
    val overlapChars: Int,
)

data class OllamaProperties (
    val baseUrl: String?  = null,
    val temperature: Double,
    val topK: Int,
)

data class ScoringProperties (
    val minScore: Double? = null,
    val list: MutableList<ScoringItemProperties> = mutableListOf(),
)

data class ScoringItemProperties(
    val type: ScoringType,
    val name: String,
    val pathToModel: String? = null,
    val pathToTokenizer: String? = null,
    val modelName: String? = null,
    val temperature: Double? = null,
)

