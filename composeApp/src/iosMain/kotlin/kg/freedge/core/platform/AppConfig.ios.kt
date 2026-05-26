package kg.freedge.core.platform

import platform.Foundation.NSBundle

actual object AppConfig {
    actual val groqApiKey: String
        get() = NSBundle.mainBundle.infoDictionary?.get("GROQ_API_KEY") as? String ?: ""
    actual val pexelsApiKey: String
        get() = NSBundle.mainBundle.infoDictionary?.get("PEXELS_API_KEY") as? String ?: ""
    actual val isDebug: Boolean = Platform.isDebugBinary
}
