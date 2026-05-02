package kg.freedge.shared.data.parser

import kg.freedge.shared.FridgeAnalysis
import kg.freedge.shared.RecipeImageQuery

internal class FridgeAnalysisParser {
    fun parse(text: String): FridgeAnalysis {
        val queries = IMAGE_QUERY_BLOCK.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.lineSequence()
            ?.map { it.trim().trim('-', '*') }
            ?.mapNotNull { parseImageQueryLine(it) }
            ?.distinctBy { it.query.lowercase() }
            ?.take(MAX_IMAGES)
            ?.toList()
            .orEmpty()

        val displayText = text
            .replace(IMAGE_QUERY_BLOCK, "")
            .replace(Regex("^\\s*ШАГ\\s*\\d+[^\\n]*\\n", RegexOption.MULTILINE), "")
            .replace(Regex("^\\s*STEP\\s*\\d+[^\\n]*\\n", RegexOption.MULTILINE), "")
            .trim()

        return FridgeAnalysis(
            displayText = displayText,
            imageQueries = queries
        )
    }

    private fun parseImageQueryLine(line: String): RecipeImageQuery? {
        val title = Regex("title:\\s*([^|]+)", RegexOption.IGNORE_CASE)
            .find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        val query = Regex("query:\\s*(.+)$", RegexOption.IGNORE_CASE)
            .find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()

        if (!query.isNullOrBlank()) {
            return RecipeImageQuery(
                title = title?.takeIf { it.isNotBlank() } ?: query,
                query = query
            )
        }

        val fallback = line.removePrefix("query:").trim()
        return fallback.takeIf { it.isNotBlank() }?.let {
            RecipeImageQuery(title = it, query = it)
        }
    }

    private companion object {
        private const val MAX_IMAGES = 3
        private val IMAGE_QUERY_BLOCK = Regex(
            pattern = "<!--\\s*freedge_image_queries\\s*(.*?)\\s*-->",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
}
