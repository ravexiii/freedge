package kg.freedge.shared.data.prompt

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal object FridgePromptProvider {
    fun systemPrompt(languageCode: String): String =
        if (languageCode.lowercase().startsWith("ru")) {
            decodeBase64(SYSTEM_PROMPT_RU)
        } else {
            SYSTEM_PROMPT_EN
        }

    fun userPrompt(languageCode: String): String =
        if (languageCode.lowercase().startsWith("ru")) {
            decodeBase64(USER_PROMPT_RU)
        } else {
            USER_PROMPT_EN
        }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64(value: String): String = Base64.decode(value).decodeToString()

    private const val SYSTEM_PROMPT_EN =
        "You are a careful kitchen vision assistant. Name only products that are actually visible. " +
            "Do not invent missing ingredients. Suggest recipes only from confidently visible products. " +
            "For each suggested dish, add a short English stock-photo search query inside the hidden HTML block."

    private const val USER_PROMPT_EN =
        "Analyze this fridge photo. First identify visible products, then suggest 1-3 practical recipes. " +
            "Use markdown with short lists. At the end add a hidden block exactly like this:\n" +
            "<!-- freedge_image_queries\n" +
            "title: Tomato omelette | query: simple omelette with tomatoes\n" +
            "title: Vegetable salad | query: vegetable salad bowl\n" +
            "-->\n" +
            "The title must match the dish name, and the query must be in English."

    private const val SYSTEM_PROMPT_RU =
        "0KLRiyDQsNC60LrRg9GA0LDRgtC90YvQuSDQstC40LfRg9Cw0LvRjNC90YvQuSDQsNGB0YHQuNGB0YLQtdC90YIg0LTQu9GPINC60YPRhdC90LguINCd0LDQt9GL0LLQsNC5INGC0L7Qu9GM0LrQviDQv9GA0L7QtNGD0LrRgtGLLCDQutC+0YLQvtGA0YvQtSDQtNC10LnRgdGC0LLQuNGC0LXQu9GM0L3QviDQstC40LTQvdGLINC90LAg0YTQvtGC0L4uINCd0LUg0L/RgNC40LTRg9C80YvQstCw0Lkg0L3QtdC00L7RgdGC0LDRjtGJ0LjQtSDQuNC90LPRgNC10LTQuNC10L3RgtGLLiDQn9GA0LXQtNC70LDQs9Cw0Lkg0YDQtdGG0LXQv9GC0Ysg0YLQvtC70YzQutC+INC40Lcg0YPQstC10YDQtdC90L3QviDRgNCw0YHQv9C+0LfQvdCw0L3QvdGL0YUg0L/RgNC+0LTRg9C60YLQvtCyLiDQlNC70Y8g0LrQsNC20LTQvtCz0L4g0LHQu9GO0LTQsCDQtNC+0LHQsNCy0Ywg0LrQvtGA0L7RgtC60LjQuSDQsNC90LPQu9C40LnRgdC60LjQuSDQt9Cw0L/RgNC+0YEg0LTQu9GPIHN0b2NrIHBob3RvINCy0L3Rg9GC0YDQuCDRgdC60YDRi9GC0L7Qs9C+IEhUTUwt0LHQu9C+0LrQsC4="

    private const val USER_PROMPT_RU =
        "0J/RgNC+0LDQvdCw0LvQuNC30LjRgNGD0Lkg0YTQvtGC0L4g0YXQvtC70L7QtNC40LvRjNC90LjQutCwLiDQodC90LDRh9Cw0LvQsCDQv9C10YDQtdGH0LjRgdC70Lgg0LLQuNC00LjQvNGL0LUg0L/RgNC+0LTRg9C60YLRiywg0LfQsNGC0LXQvCDQv9GA0LXQtNC70L7QttC4IDEtMyDQv9GA0LDQutGC0LjRh9C90YvRhSDRgNC10YbQtdC/0YLQsC4g0J7RgtCy0LXRh9Cw0Lkg0L3QsCDRgNGD0YHRgdC60L7QvCwg0LjRgdC/0L7Qu9GM0LfRg9C5IG1hcmtkb3duINC4INC60L7RgNC+0YLQutC40LUg0YHQv9C40YHQutC4LiDQkiDRgdCw0LzQvtC8INC60L7QvdGG0LUg0LTQvtCx0LDQstGMINGB0LrRgNGL0YLRi9C5INCx0LvQvtC6INGB0YLRgNC+0LPQviDQsiDRgtCw0LrQvtC8INGE0L7RgNC80LDRgtC1Ogo8IS0tIGZyZWVkZ2VfaW1hZ2VfcXVlcmllcwp0aXRsZTog0J7QvNC70LXRgiDRgSDRgtC+0LzQsNGC0LDQvNC4IHwgcXVlcnk6IHNpbXBsZSBvbWVsZXR0ZSB3aXRoIHRvbWF0b2VzCnRpdGxlOiDQntCy0L7RidC90L7QuSDRgdCw0LvQsNGCIHwgcXVlcnk6IHZlZ2V0YWJsZSBzYWxhZCBib3dsCi0tPgrQndCw0LfQstCw0L3QuNC1INC00L7Qu9C20L3QviDRgdC+0LLQv9Cw0LTQsNGC0Ywg0YEg0LHQu9GO0LTQvtC8LCDQsCBxdWVyeSDQtNC+0LvQttC10L0g0LHRi9GC0Ywg0L3QsCDQsNC90LPQu9C40LnRgdC60L7QvC4="
}
