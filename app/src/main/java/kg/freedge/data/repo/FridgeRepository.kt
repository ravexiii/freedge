package kg.freedge.data.repo

import android.util.Base64
import com.google.gson.JsonObject
import kg.freedge.BuildConfig
import kg.freedge.data.api.GroqApi
import kg.freedge.data.api.GroqChatRequest
import kg.freedge.data.api.GroqMessage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FridgeRepository {

    private val api: GroqApi

    init {
        // Не BODY: тело запроса — огромный base64 JPEG
        val logging = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(GroqApi::class.java)
    }

    suspend fun analyzeImage(imageBytes: ByteArray, apiKey: String): Result<String> {
        return try {
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val request = GroqChatRequest(
                model = VISION_MODEL,
                messages = listOf(
                    buildUserMessage(
                        prompt = """
                            Посмотри на фото холодильника/продуктов.

                            1. Перечисли что видишь (кратко)
                            2. Предложи 2-3 рецепта из этих продуктов

                            Отвечай на русском, коротко и по делу.
                        """.trimIndent(),
                        base64Jpeg = base64Image
                    )
                )
            )

            val response = api.chatCompletions(
                authorization = "Bearer $apiKey",
                request = request
            )
            val text = response.choices?.firstOrNull()?.message?.content
                ?: "Не удалось получить ответ"

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildUserMessage(prompt: String, base64Jpeg: String): GroqMessage {
        val textPart = JsonObject().apply {
            addProperty("type", "text")
            addProperty("text", prompt)
        }
        val imagePart = JsonObject().apply {
            addProperty("type", "image_url")
            val imageUrl = JsonObject()
            imageUrl.addProperty("url", "data:image/jpeg;base64,$base64Jpeg")
            add("image_url", imageUrl)
        }
        return GroqMessage(role = "user", content = listOf(textPart, imagePart))
    }

    companion object {
        private const val VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct"
    }
}
