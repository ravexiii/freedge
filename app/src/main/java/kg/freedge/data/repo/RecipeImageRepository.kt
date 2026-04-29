package kg.freedge.data.repo

import kg.freedge.data.api.PexelsApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class RecipeImage(
    val title: String,
    val query: String,
    val imageUrl: String,
    val photographer: String,
    val sourceUrl: String
)

class RecipeImageRepository {

    private val api: PexelsApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.pexels.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(PexelsApi::class.java)
    }

    suspend fun searchRecipeImages(
        imageQueries: List<RecipeImageQuery>,
        apiKey: String
    ): List<RecipeImage> {
        if (apiKey.isBlank()) return emptyList()

        return imageQueries
            .filter { it.query.isNotBlank() }
            .distinctBy { it.query.lowercase() }
            .take(MAX_IMAGES)
            .mapNotNull { imageQuery ->
                runCatching {
                    val photo = api.searchPhotos(
                        authorization = apiKey,
                        query = imageQuery.query,
                        perPage = 1
                    ).photos?.firstOrNull() ?: return@runCatching null

                    val imageUrl = photo.src.landscape ?: photo.src.large ?: photo.src.medium
                        ?: return@runCatching null

                    RecipeImage(
                        title = imageQuery.title,
                        query = imageQuery.query,
                        imageUrl = imageUrl,
                        photographer = photo.photographer,
                        sourceUrl = photo.url
                    )
                }.getOrNull()
            }
    }

    companion object {
        private const val MAX_IMAGES = 3
    }
}
