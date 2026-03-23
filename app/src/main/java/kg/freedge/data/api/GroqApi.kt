package kg.freedge.data.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: GroqChatRequest
    ): GroqChatResponse
}

data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val max_completion_tokens: Int = 1024,
    val temperature: Double = 0.7
)

data class GroqMessage(
    val role: String,
    val content: JsonElement
)

data class GroqChatResponse(
    val choices: List<GroqChoice>?
)

data class GroqChoice(
    val message: GroqAssistantMessage?
)

data class GroqAssistantMessage(
    val role: String? = null,
    val content: String?
)
