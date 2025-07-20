package org.jbareaud.ragchat.ai.reranker

import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.chat.request.ResponseFormatType
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonSchema
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.model.scoring.ScoringModel


/**
 * Testing Ollama models as reranker models
 */
class OllamaScoringModel(builder: OllamaScoringModelBuilder) : ScoringModel {

    companion object {
        private const val SYSTEM_PROMPT = """
           You are an expert relevance ranker. 
           Given a document and a query, your job is to determine how relevant the document is for answering the query. 
           Your output is JSON, which contains two fields, content and score.  
           score is from 0.00 to 1.00.
           Higher relevance means higher score.
       """

        fun builder() = OllamaScoringModelBuilder()
    }

    private val chatModel = OllamaChatModel.builder()
        .baseUrl(builder.baseUrl)
        .modelName(builder.modelName)
        .temperature(builder.temperature)
        .build()

    private val responseFormat = ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(
            JsonSchema.builder()
                .name("Score")
                .rootElement(
                    JsonObjectSchema.builder()
                        .addStringProperty("content")
                        .addNumberProperty("score")
                        .build()
                )
                .build()
        )
        .build()

    private val mapper = ObjectMapper()

    override fun scoreAll(segments: List<TextSegment>, query: String): Response<List<Double>> {
        val scores = mutableListOf<Double>()
        for (segment in segments) {

            val chatRequest: ChatRequest = ChatRequest.builder()
                .responseFormat(responseFormat)
                .messages(listOf(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from("Query: $query Document: ${segment.text()}")
                ))
                .build()

            val response = chatModel.chat(chatRequest)
                .aiMessage()
                .text()

            val score = mapper.readTree(response)
                ?.get("score")
                ?.asDouble() ?: 0.0

            scores.add(score)
        }
        return Response.from(scores)
    }

    class OllamaScoringModelBuilder {

        var baseUrl: String? = null
            private set
        var modelName: String? = null
            private set
        var temperature: Double? = null
            private set

        fun baseUrl(baseUrl: String?) = apply { this.baseUrl = baseUrl }

        fun modelName(modelName: String?) = apply { this.modelName = modelName }

        fun temperature(temperature: Double) = apply { this.temperature = temperature }

        fun build() = OllamaScoringModel(this)
    }
}
