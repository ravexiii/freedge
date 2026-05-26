package kg.freedge.feature.main

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun isRussian(): Boolean = NSLocale.currentLocale.languageCode == "ru"
actual fun currentLanguageCode(): String = NSLocale.currentLocale.languageCode
