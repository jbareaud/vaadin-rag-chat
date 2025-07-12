package org.jbareaud.ragchat.ai

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "rag-chat")
data class ConfigProperties @ConstructorBinding constructor(
    val chatOllamaBaseUrl: String?  = null,
    val chatTemperature: Double,
    val scoringEnabled: Boolean,
    val scoringPathToModel: String? = null,
    val scoringPathToTokenizer: String? = null,
    val scoringMinScore: Double? = null,
    val contentRetrieverMaxResults: Int,
    val memoryProviderMaxMessages: Int,
    val splitterMaxChars: Int,
    val splitterOverlapChars: Int,
)