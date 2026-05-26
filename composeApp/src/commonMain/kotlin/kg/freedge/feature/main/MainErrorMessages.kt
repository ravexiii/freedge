package kg.freedge.feature.main

import kg.freedge.shared.FreedgeErrorCode
import kg.freedge.shared.FreedgeException

object MainErrorMessages {
    val noInternet: String get() = if (isRussian()) "Нет интернета. Проверьте подключение." else "No internet. Check your connection."
    val missingApiKey: String get() = if (isRussian()) "Добавьте GROQ_API_KEY в local.properties" else "Add GROQ_API_KEY to local.properties"

    fun fromException(e: Throwable): String {
        val code = (e as? FreedgeException)?.code ?: FreedgeErrorCode.Unknown
        return if (isRussian()) ruMessage(code) else enMessage(code)
    }

    private fun ruMessage(code: FreedgeErrorCode) = when (code) {
        FreedgeErrorCode.Network -> "Нет интернета. Проверьте подключение."
        FreedgeErrorCode.MissingGroqApiKey -> "Добавьте GROQ_API_KEY в настройки проекта"
        FreedgeErrorCode.EmptyResponse -> "Не удалось получить ответ"
        FreedgeErrorCode.ApiRateLimited -> "Сервис временно перегружен. Попробуйте чуть позже."
        FreedgeErrorCode.ApiAuth -> "Проверьте GROQ_API_KEY: сервис отклонил ключ."
        FreedgeErrorCode.ApiServer -> "Сервис рецептов сейчас недоступен. Попробуйте позже."
        FreedgeErrorCode.Unknown -> "Что-то пошло не так"
    }

    private fun enMessage(code: FreedgeErrorCode) = when (code) {
        FreedgeErrorCode.Network -> "No internet connection."
        FreedgeErrorCode.MissingGroqApiKey -> "Add GROQ_API_KEY to your project settings"
        FreedgeErrorCode.EmptyResponse -> "Empty response from server"
        FreedgeErrorCode.ApiRateLimited -> "Service is overloaded. Try again in a moment."
        FreedgeErrorCode.ApiAuth -> "API key rejected. Check your GROQ_API_KEY."
        FreedgeErrorCode.ApiServer -> "Recipe service is down. Try later."
        FreedgeErrorCode.Unknown -> "Something went wrong"
    }
}

expect fun isRussian(): Boolean
expect fun currentLanguageCode(): String
