package kg.freedge.data.repo

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
    cause: Throwable? = null,
    message: String = code.name
) : Exception(message, cause)
