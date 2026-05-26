package kg.freedge.core.platform

import kg.freedge.BuildConfig

actual object AppConfig {
    actual val groqApiKey: String = BuildConfig.GROQ_API_KEY
    actual val pexelsApiKey: String = BuildConfig.PEXELS_API_KEY
    actual val isDebug: Boolean = BuildConfig.DEBUG
}
