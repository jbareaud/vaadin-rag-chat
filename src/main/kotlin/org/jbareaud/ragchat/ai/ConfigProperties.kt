package org.jbareaud.ragchat.ai

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import java.time.Duration

@ConfigurationProperties(prefix = "rag-chat")
data class ConfigProperties @ConstructorBinding constructor(
    val chatOllamaBaseUrl: String?  = null,
    val chatTemperature: Double,
    val chatTopK: Int,
    val scoringEnabled: Boolean,
    val scoringPathToModel: String? = null,
    val scoringPathToTokenizer: String? = null,
    val scoringMinScore: Double? = null,
    val contentRetrieverMaxResults: Int,
    val memoryProviderMaxMessages: Int,
    val splitterMaxChars: Int,
    val splitterOverlapChars: Int,
    val chromaEnabled: Boolean,
    val chromaBaseUrl: String?,
    val chromaClientTimeout: Duration?,
    val chromaStoreTimeout: Duration?,
)