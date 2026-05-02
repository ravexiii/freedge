package kg.freedge.shared

data class FridgeAnalysis(
    val displayText: String,
    val imageQueries: List<RecipeImageQuery>
)

data class FridgeScanResult(
    val analysis: FridgeAnalysis,
    val recipeImages: List<RecipeImage>
)

data class RecipeImageQuery(
    val title: String,
    val query: String
)

data class RecipeImage(
    val title: String,
    val query: String,
    val imageUrl: String,
    val photographer: String,
    val sourceUrl: String
)

enum class FreedgeErrorCode {
    MissingGroqApiKey,
    EmptyResponse,
    ApiAuth,
    ApiRateLimited,
    ApiServer,
    Network,
    Unknown
}

class FreedgeException(
    val code: FreedgeErrorCode,
    message: String = code.name,
    cause: Throwable? = null
) : Exception(message, cause)
