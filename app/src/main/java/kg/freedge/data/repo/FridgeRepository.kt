package kg.freedge.data.repo

import android.util.Base64
import kg.freedge.data.api.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FridgeRepository {

    private val api: GeminiApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(GeminiApi::class.java)
    }

    suspend fun analyzeImage(imageBytes: ByteArray, apiKey: String): Result<String> {
        return try {
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                inline_data = GeminiInlineData(data = base64Image)
                            ),
                            GeminiPart(
                                text = """
                                    Посмотри на фото холодильника/продуктов.
                                    
                                    1. Перечисли что видишь (кратко)
                                    2. Предложи 2-3 рецепта из этих продуктов
                                    
                                    Отвечай на русском, коротко и по делу.
                                """.trimIndent()
                            )
                        )
                    )
                )
            )

            val response = api.analyze(apiKey, request)
            val text = response.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: "Не удалось получить ответ"

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}