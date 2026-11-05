package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GoogleSearchRetrieval(
    @Json(name = "google_search") val googleSearch: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val tools: List<GoogleSearchRetrieval>? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    val message: String? = null,
    val code: Int? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Executa a chamada ao Gemini habilitando o Google Search Grounding (pesquisa em tempo real
     * de equipamento real do guitarrista, BPM oficial da faixa e timbre de gravação).
     * Caso a chave não suporte a tool de busca em determinado plano, faz fallback automático
     * transparente para a geração direta.
     */
    suspend fun generateWithSearchGrounding(
        apiKey: String,
        contents: List<Content>,
        systemInstruction: Content? = null
    ): GenerateContentResponse {
        return try {
            val groundedRequest = GenerateContentRequest(
                contents = contents,
                systemInstruction = systemInstruction,
                tools = listOf(GoogleSearchRetrieval())
            )
            val response = service.generateContent(apiKey, groundedRequest)
            if (!response.candidates.isNullOrEmpty()) {
                response
            } else {
                service.generateContent(
                    apiKey,
                    GenerateContentRequest(contents = contents, systemInstruction = systemInstruction)
                )
            }
        } catch (e: Exception) {
            service.generateContent(
                apiKey,
                GenerateContentRequest(contents = contents, systemInstruction = systemInstruction)
            )
        }
    }
}
