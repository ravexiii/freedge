package kg.freedge.core.platform

expect object AppConfig {
    val groqApiKey: String
    val pexelsApiKey: String
    val isDebug: Boolean
}
