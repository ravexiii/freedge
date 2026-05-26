package kg.freedge.feature.main

import java.util.Locale

actual fun isRussian(): Boolean = Locale.getDefault().language == "ru"
actual fun currentLanguageCode(): String = Locale.getDefault().language
