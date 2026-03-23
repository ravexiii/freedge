package kg.freedge.data.repo

import android.util.Base64
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kg.freedge.BuildConfig
import kg.freedge.data.ImageUtils
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
            val compressed = ImageUtils.compressForUpload(imageBytes)
            val base64Image = Base64.encodeToString(compressed, Base64.NO_WRAP)

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

            Result.success(cleanResponse(text))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanResponse(text: String): String =
        text.replace(Regex("^\\s*ШАГ\\s*\\d+[^\\n]*\\n", RegexOption.MULTILINE), "")
            .replace(Regex("^\\s*STEP\\s*\\d+[^\\n]*\\n", RegexOption.MULTILINE), "")
            .trim()

    private fun buildSystemMessage(): GroqMessage =
        GroqMessage(role = "system", content = JsonPrimitive(SYSTEM_PROMPT))

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
        val contentArray = JsonArray().apply {
            add(textPart)
            add(imagePart)
        }
        return GroqMessage(role = "user", content = contentArray)
    }

    companion object {
        private const val VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct"

        private val SYSTEM_PROMPT = """
Ты — опытный шеф-повар и кулинарный консультант. Твоя задача — помочь людям готовить вкусные блюда из того, что у них есть.

Твои принципы:
- Ты обожаешь готовить и делиться рецептами
- Ты креативен, но практичен — предлагаешь реальные блюда
- Ты честен — если не видишь продукт чётко, говоришь об этом
- Базовые продукты (соль, перец, масло, вода, специи) есть на любой кухне

Формат ответа: используй markdown — заголовки (##), жирный текст (**), списки (-).
Отвечай на русском языке.
        """.trimIndent()

        private val USER_PROMPT = """
Посмотри на фото и помоги приготовить что-нибудь вкусное.

**Продукты на фото:**
Перечисли что видишь. Если продукт в упаковке и непонятно что — опиши упаковку.

**Рецепты:**
Предложи 2-3 реальных блюда из этих продуктов. Для каждого укажи название, время готовки, какие продукты с фото используются, и краткие шаги (3-5).

Если продуктов мало — предложи что докупить для хорошего ужина.
        """.trimIndent()
    }
}
