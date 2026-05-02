package kg.freedge.shared.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int = 1024,
    val temperature: Double = 0.7
)

@Serializable
internal data class GroqMessage(
    val role: String,
    val content: JsonElement
)

@Serializable
internal data class GroqChatResponse(
    val choices: List<GroqChoice>? = null
)

@Serializable
internal data class GroqChoice(
    val message: GroqAssistantMessage? = null
)

@Serializable
internal data class GroqAssistantMessage(
    val role: String? = null,
    val content: String? = null
)
