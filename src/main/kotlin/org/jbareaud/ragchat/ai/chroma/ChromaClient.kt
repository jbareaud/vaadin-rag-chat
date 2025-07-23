package org.jbareaud.ragchat.ai.chroma

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.langchain4j.internal.Utils
import okhttp3.OkHttpClient
import org.jbareaud.ragchat.ai.ConfigProperties
import org.jbareaud.ragchat.logger
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import java.io.IOException
import java.time.Duration

/**
 * retrofit web client to query list of existing collections from Chroma
 *
 *  @see dev.langchain4j.store.embedding.chroma.ChromaClient (langchain4j package private client)
 */
class ChromaClient(props: ConfigProperties) {

    private val api: ChromaPartialApi

    init {
        val httpClientBuilder = OkHttpClient.Builder()
            .callTimeout(props.chroma?.clientTimeout.orDefault())
            .connectTimeout(props.chroma?.clientTimeout.orDefault())
            .readTimeout(props.chroma?.clientTimeout.orDefault())
            .writeTimeout(props.chroma?.clientTimeout.orDefault())

        val objectMapper = ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModules(KotlinModule.Builder().build())

        val retrofit = Retrofit.Builder()
            .baseUrl(Utils.ensureTrailingForwardSlash(props.chroma?.baseUrl))
            .client(httpClientBuilder.build())
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build()

        api = retrofit.create(ChromaPartialApi::class.java)
    }

    fun collectionNames(): List<String>? {
        try {
            val response: retrofit2.Response<List<Collection>> = api.collections().execute()
            if (response.isSuccessful) {
                return response.body()?.map { it.name }
            } else {
                throw RuntimeException("[ChromaClient] retrofit call error code ${response.code()}")
            }
        } catch (err: IOException) {
            val message = "[ChromaClient] retrofit call in error"
            logger().error("$message : $err")
            throw RuntimeException(message, err)
        }
    }
}

private fun Duration?.orDefault() = this ?: Duration.ofMillis(60000)

data class Collection(
    val id: String? = null,
    val name: String,
    val metadata: Map<String, Any>? = null
)

interface ChromaPartialApi {
    @GET("api/v1/collections")
    @Headers("Content-Type: application/json")
    fun collections(): retrofit2.Call<List<Collection>>
}
