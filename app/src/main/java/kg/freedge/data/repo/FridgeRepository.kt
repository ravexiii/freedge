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
import java.util.Locale

class FridgeRepository {

    private val api: GroqApi

    init {
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
            .baseUrl("https://api.groq.com/")
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
                    buildSystemMessage(buildSystemPrompt()),
                    buildUserMessage(
                        prompt = buildUserPrompt(),
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

    private fun getSystemLanguage(): String = Locale.getDefault().language

    private fun buildSystemPrompt(): String =
        if (getSystemLanguage() == "ru") {
            """
Ты — опытный шеф-повар. Ты обожаешь готовить и помогать людям создавать вкусные блюда.
Отвечай на русском языке. Используй markdown для форматирования.
            """.trimIndent()
        } else {
            """
You are an experienced chef. You love cooking and helping people create delicious meals.
Respond in English. Use markdown for formatting.
            """.trimIndent()
        }

    private fun buildUserPrompt(): String =
        if (getSystemLanguage() == "ru") {
            """
Посмотри на фото и помоги приготовить что-нибудь вкусное.

**Продукты на фото:**
Перечисли что видишь. Если продукт в упаковке и непонятно что внутри — опиши упаковку.

**Рецепты:**
Предложи 2-3 блюда из этих продуктов. Для каждого:
- Название и время готовки
- Ингредиенты (с фото + базовые)
- Шаги приготовления (3-5 шагов)

Если продуктов мало — предложи что докупить.
            """.trimIndent()
        } else {
            """
Look at this photo and help me cook something delicious.

**Products in the photo:**
List what you see. If a product is in packaging and you can't tell what's inside — describe the packaging.

**Recipes:**
Suggest 2-3 dishes from these products. For each:
- Name and cooking time
- Ingredients (from photo + basics)
- Cooking steps (3-5 steps)

If there aren't enough products — suggest what to buy.
            """.trimIndent()
        }

    private fun buildSystemMessage(prompt: String): GroqMessage =
        GroqMessage(role = "system", content = JsonPrimitive(prompt))

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
    }
}