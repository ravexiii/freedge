package kg.freedge.shared

import io.ktor.client.HttpClient
import kg.freedge.shared.data.network.createFreedgeHttpClient
import kg.freedge.shared.data.repository.FreedgeRepository

class FreedgeSharedClient internal constructor(
    private val repository: FreedgeRepository
) {
    constructor() : this(FreedgeRepository(createFreedgeHttpClient()))

    internal constructor(httpClient: HttpClient) : this(FreedgeRepository(httpClient))

    @Throws(FreedgeException::class)
    suspend fun analyzeImage(
        imageBytes: ByteArray,
        groqApiKey: String,
        languageCode: String = "en"
    ): FridgeAnalysis = repository.analyzeImage(imageBytes, groqApiKey, languageCode)

    @Throws(FreedgeException::class)
    suspend fun searchRecipeImages(
        imageQueries: List<RecipeImageQuery>,
        pexelsApiKey: String
    ): List<RecipeImage> = repository.searchRecipeImages(imageQueries, pexelsApiKey)

    @Throws(FreedgeException::class)
    suspend fun analyzeImageWithRecipeImages(
        imageBytes: ByteArray,
        groqApiKey: String,
        pexelsApiKey: String,
        languageCode: String = "en"
    ): FridgeScanResult {
        val analysis = analyzeImage(imageBytes, groqApiKey, languageCode)
        return FridgeScanResult(
            analysis = analysis,
            recipeImages = searchRecipeImages(analysis.imageQueries, pexelsApiKey)
        )
    }

    fun close() {
        repository.close()
    }
}
