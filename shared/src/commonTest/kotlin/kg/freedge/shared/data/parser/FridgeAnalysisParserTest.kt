package kg.freedge.shared.data.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FridgeAnalysisParserTest {
    private val parser = FridgeAnalysisParser()

    @Test
    fun parsesHiddenImageQueriesAndRemovesBlockFromDisplayText() {
        val result = parser.parse(
            """
            ## What I can see
            - tomatoes
            - eggs

            <!-- freedge_image_queries
            title: Tomato omelette | query: simple omelette with tomatoes
            title: Vegetable salad | query: vegetable salad bowl
            -->
            """.trimIndent()
        )

        assertEquals(
            """
            ## What I can see
            - tomatoes
            - eggs
            """.trimIndent(),
            result.displayText
        )
        assertEquals(2, result.imageQueries.size)
        assertEquals("Tomato omelette", result.imageQueries[0].title)
        assertEquals("simple omelette with tomatoes", result.imageQueries[0].query)
        assertEquals("Vegetable salad", result.imageQueries[1].title)
        assertEquals("vegetable salad bowl", result.imageQueries[1].query)
    }

    @Test
    fun keepsOnlyThreeDistinctImageQueries() {
        val result = parser.parse(
            """
            Recipe text

            <!-- freedge_image_queries
            title: First | query: simple soup
            title: Duplicate | query: simple soup
            title: Second | query: tomato salad
            title: Third | query: vegetable pasta
            title: Fourth | query: rice bowl
            -->
            """.trimIndent()
        )

        assertEquals(
            listOf("simple soup", "tomato salad", "vegetable pasta"),
            result.imageQueries.map { it.query }
        )
    }

    @Test
    fun stripsStepHeadingsFromDisplayText() {
        val result = parser.parse(
            """
            STEP 1 Ingredients
            - eggs
            ШАГ 2 Рецепт
            - cook
            """.trimIndent()
        )

        assertFalse(result.displayText.contains("STEP 1"))
        assertFalse(result.displayText.contains("ШАГ 2"))
        assertEquals(
            """
            - eggs
            - cook
            """.trimIndent(),
            result.displayText
        )
    }
}
