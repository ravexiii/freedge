package kg.freedge.feature.history

import java.text.SimpleDateFormat
import java.util.*

actual fun formatDate(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))

actual fun stripMarkdown(text: String): String =
    text.replace(Regex("[#*_`~>\\[\\]()!]"), "").trim()
