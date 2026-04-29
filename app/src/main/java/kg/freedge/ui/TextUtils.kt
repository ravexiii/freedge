package kg.freedge.ui

fun String.stripMarkdown(): String = this
    .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
    .replace(Regex("\\*{1,3}(.+?)\\*{1,3}"), "$1")
    .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "")
    .replace(Regex("\\[(.+?)]\\(.+?\\)"), "$1")
    .trim()
