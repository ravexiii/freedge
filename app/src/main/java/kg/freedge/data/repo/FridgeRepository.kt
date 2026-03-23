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
                    buildSystemMessage(),
                    buildUserMessage(
                        prompt = USER_PROMPT,
                        base64Jpeg = base64Image
                    )
                ),
                temperature = 0.7
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

    private fun buildSystemMessage(): GroqMessage {
        val textPart = JsonObject().apply {
            addProperty("type", "text")
            addProperty("text", SYSTEM_PROMPT)
        }
        return GroqMessage(role = "system", content = listOf(textPart))
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

        private val SYSTEM_PROMPT = """
Ты — опытный шеф-повар и кулинарный консультант. Твоя задача — помочь людям готовить вкусные блюда из того, что у них есть.

Твои принципы:
- Ты ОБОЖАЕШЬ готовить и делиться рецептами
- Ты креативен, но практичен — предлагаешь реальные блюда
- Ты честен — если не видишь продукт чётко, говоришь об этом
- Базовые продукты (соль, перец, масло, вода, специи) есть на любой кухне

Ты отвечаешь на русском языке.
        """.trimIndent()

        private val USER_PROMPT = """
Посмотри на это фото холодильника/продуктов.

ШАГ 1 — РАСПОЗНАЙ ПРОДУКТЫ:
Перечисли что видишь. Если продукт в упаковке и не понятно что — опиши упаковку.

ШАГ 2 — ПРЕДЛОЖИ РЕЦЕПТЫ:
Придумай 2-3 реальных вкусных блюда из этих продуктов.
Для каждого рецепта укажи:
- Название и время готовки
- Какие продукты с фото используются
- Краткие шаги (3-5)

Будь креативным! Комбинируй продукты интересно. Если есть крупы + что-то ещё — это уже основа для полноценного блюда.

Если продуктов совсем мало — предложи что докупить для хорошего ужина.
        """.trimIndent()
    }
}
