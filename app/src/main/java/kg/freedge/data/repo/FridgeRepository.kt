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
                            Ты — помощник по кулинарии. Анализируешь фото холодильника.

                            ## СТРОГИЕ ПРАВИЛА РАСПОЗНАВАНИЯ:
                            - Называй ТОЛЬКО то, что ТОЧНО видишь и можешь идентифицировать
                            - Если видишь упаковку но не понимаешь что внутри -> "продукт в [цвет] упаковке"
                            - Если видишь контейнер/тарелку с едой но не понятно что -> "готовое блюдо" или пропусти
                            - НЕ УГАДЫВАЙ. НЕ ДОДУМЫВАЙ. Лучше пропустить чем ошибиться
                            - Указывай примерное количество если видно (1 шт, пачка, немного)

                            ## ПРАВИЛА РЕЦЕПТОВ:
                            - Предлагай рецепты ТОЛЬКО из распознанных продуктов
                            - Не добавляй ингредиенты которых нет на фото (кроме соль/перец/масло — базовые)
                            - Если продуктов мало — предложи 1 простой рецепт или честно скажи что мало продуктов
                            - Рецепты должны быть реалистичными и простыми (до 30 минут готовки)
                            - Укажи краткие шаги приготовления (3-5 шагов максимум)

                            ## ФОРМАТ ОТВЕТА:

                            ### Продукты:
                            [список того что точно видишь]

                            ### Рецепты:

                            **[Название]** (~X мин)
                            Ингредиенты: [только из списка выше]
                            1. [шаг]
                            2. [шаг]
                            3. [шаг]

                            ---

                            Отвечай на русском. Будь честным — если мало продуктов или плохо видно, так и скажи.
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
