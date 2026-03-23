package kg.freedge.data.api

data class ClaudeRequest(
    val model: String = "claude-sonnet-4-20250514",
    val max_tokens: Int = 1024,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: List<Content>
)

data class Content(
    val type: String,
    val text: String? = null,
    val source: ImageSource? = null
)

data class ImageSource(
    val type: String = "base64",
    val media_type: String = "image/jpeg",
    val data: String
)

data class ClaudeResponse(
    val content: List<ResponseContent>
)

data class ResponseContent(
    val type: String,
    val text: String?
)