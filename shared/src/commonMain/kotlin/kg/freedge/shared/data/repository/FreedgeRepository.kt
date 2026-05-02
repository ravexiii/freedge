package kg.freedge.shared.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kg.freedge.shared.FreedgeErrorCode
import kg.freedge.shared.FreedgeException
import kg.freedge.shared.FridgeAnalysis
import kg.freedge.shared.RecipeImage
import kg.freedge.shared.RecipeImageQuery
import kg.freedge.shared.data.network.FreedgeNetworkErrorMapper
import kg.freedge.shared.data.network.GroqChatRequest
import kg.freedge.shared.data.network.GroqChatResponse
import kg.freedge.shared.data.network.GroqMessage
import kg.freedge.shared.data.network.PexelsSearchResponse
import kg.freedge.shared.data.parser.FridgeAnalysisParser
import kg.freedge.shared.data.prompt.FridgePromptProvider

internal class FreedgeRepository(
    private val httpClient: HttpClient,
    private val analysisParser: FridgeAnalysisParser = FridgeAnalysisParser()
) {
    @Throws(FreedgeException::class)
    suspend fun analyzeImage(
        imageBytes: ByteArray,
        groqApiKey: String,
        languageCode: String
    ): FridgeAnalysis {
        if (groqApiKey.isBlank()) {
            throw FreedgeException(FreedgeErrorCode.MissingGroqApiKey)
        }

        return runFreedgeCall {
            val response = httpClient.post("$GROQ_BASE_URL/openai/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $groqApiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    GroqChatRequest(
                        model = VISION_MODEL,
                        messages = listOf(
                            GroqMessage(
                                role = "system",
                                content = JsonPrimitive(FridgePromptProvider.systemPrompt(languageCode))
                            ),
                            GroqMessage(
                                role = "user",
                                content = buildUserContent(
                                    prompt = FridgePromptProvider.userPrompt(languageCode),
                                    base64Jpeg = imageBytes.toBase64()
                                )
                            )
                        ),
                        temperature = 0.15
                    )
                )
            }.body<GroqChatResponse>()

            val text = response.choices?.firstOrNull()?.message?.content
                ?: throw FreedgeException(FreedgeErrorCode.EmptyResponse)

            analysisParser.parse(text)
        }
    }

    @Throws(FreedgeException::class)
    suspend fun searchRecipeImages(
        imageQueries: List<RecipeImageQuery>,
        pexelsApiKey: String
    ): List<RecipeImage> {
        if (pexelsApiKey.isBlank() || imageQueries.isEmpty()) return emptyList()

        val images = mutableListOf<RecipeImage>()
        val distinctQueries = imageQueries
            .filter { it.query.isNotBlank() }
            .distinctBy { it.query.lowercase() }
            .take(MAX_IMAGES)

        for (imageQuery in distinctQueries) {
            val image = runCatching {
                searchSingleRecipeImage(imageQuery, pexelsApiKey)
            }.getOrNull()

            if (image != null) images += image
        }

        return images
    }

    fun close() {
        httpClient.close()
    }

    private suspend fun searchSingleRecipeImage(
        imageQuery: RecipeImageQuery,
        pexelsApiKey: String
    ): RecipeImage? = runFreedgeCall {
        val photo = httpClient.get("$PEXELS_BASE_URL/v1/search") {
            header(HttpHeaders.Authorization, pexelsApiKey)
            parameter("query", imageQuery.query)
            parameter("per_page", 1)
            parameter("orientation", "landscape")
        }.body<PexelsSearchResponse>().photos?.firstOrNull() ?: return@runFreedgeCall null

        val imageUrl = photo.src.landscape ?: photo.src.large ?: photo.src.medium
            ?: return@runFreedgeCall null

        RecipeImage(
            title = imageQuery.title,
            query = imageQuery.query,
            imageUrl = imageUrl,
            photographer = photo.photographer,
            sourceUrl = photo.url
        )
    }

    private fun buildUserContent(prompt: String, base64Jpeg: String): JsonArray =
        buildJsonArray {
            add(
                buildJsonObject {
                    put("type", "text")
                    put("text", prompt)
                }
            )
            add(
                buildJsonObject {
                    put("type", "image_url")
                    put(
                        "image_url",
                        buildJsonObject {
                            put("url", "data:image/jpeg;base64,$base64Jpeg")
                        }
                    )
                }
            )
        }

    private suspend fun <T> runFreedgeCall(block: suspend () -> T): T =
        try {
            block()
        } catch (error: FreedgeException) {
            throw error
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw FreedgeNetworkErrorMapper.map(error)
        }

    @OptIn(ExperimentalEncodingApi::class)
    private fun ByteArray.toBase64(): String = Base64.encode(this)

    private companion object {
        private const val GROQ_BASE_URL = "https://api.groq.com"
        private const val PEXELS_BASE_URL = "https://api.pexels.com"
        private const val VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct"
        private const val MAX_IMAGES = 3
    }
}
