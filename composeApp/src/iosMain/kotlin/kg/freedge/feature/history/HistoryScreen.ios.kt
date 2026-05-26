package kg.freedge.feature.history

import platform.Foundation.*

actual fun formatDate(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = "d MMM, HH:mm"
    formatter.locale = NSLocale.currentLocale
    return formatter.stringFromDate(date)
}

actual fun stripMarkdown(text: String): String =
    text.replace(Regex("[#*_`~>\\[\\]()!]"), "").trim()
