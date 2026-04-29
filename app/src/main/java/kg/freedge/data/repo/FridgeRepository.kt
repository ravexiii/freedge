package kg.freedge.data.repo

import android.util.Base64
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kg.freedge.app.BuildConfig
import kg.freedge.data.ImageUtils
import kg.freedge.data.api.GroqApi
import kg.freedge.data.api.GroqChatRequest
import kg.freedge.data.api.GroqMessage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

data class FridgeAnalysis(
    val displayText: String,
    val imageQueries: List<RecipeImageQuery>
)

data class RecipeImageQuery(
    val title: String,
    val query: String
)

class FridgeRepository {

    private val api: GroqApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(GroqApi::class.java)
    }

    suspend fun analyzeImage(imageBytes: ByteArray, apiKey: String): Result<FridgeAnalysis> {
        return try {
            val compressed = ImageUtils.compressForUpload(imageBytes)
            val base64Image = Base64.encodeToString(compressed, Base64.NO_WRAP)

            val request = GroqChatRequest(
                model = VISION_MODEL,
                messages = listOf(
                    buildSystemMessage(buildSystemPrompt()),
                    buildUserMessage(
                        prompt = buildUserPrompt(),
                        base64Jpeg = base64Image
                    )
                ),
                temperature = 0.15
            )

            val response = api.chatCompletions(
                authorization = "Bearer $apiKey",
                request = request
            )
            val text = response.choices?.firstOrNull()?.message?.content
                ?: return Result.failure(FreedgeException(FreedgeErrorCode.EmptyResponse))

            Result.success(parseAnalysis(text))
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    private fun parseAnalysis(text: String): FridgeAnalysis {
        val queries = IMAGE_QUERY_BLOCK.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.lineSequence()
            ?.map { it.trim().trim('-', '*') }
            ?.mapNotNull { parseImageQueryLine(it) }
            ?.distinctBy { it.query.lowercase() }
            ?.take(3)
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

    private fun mapError(error: Exception): Exception {
        if (error is FreedgeException) return error

        return when (error) {
            is HttpException -> {
                val code = error.code()
                val errorCode = when (code) {
                    401, 403 -> FreedgeErrorCode.ApiAuth
                    429 -> FreedgeErrorCode.ApiRateLimited
                    in 500..599 -> FreedgeErrorCode.ApiServer
                    else -> FreedgeErrorCode.Unknown
                }
                FreedgeException(errorCode, error, "api_error_$code")
            }
            is IOException -> FreedgeException(FreedgeErrorCode.Network, error)
            else -> error
        }
    }

    private fun getSystemLanguage(): String = Locale.getDefault().language

    private fun buildSystemPrompt(): String =
        if (getSystemLanguage() == "ru") decode(P_SYS_RU) else decode(P_SYS_EN)

    private fun buildUserPrompt(): String =
        if (getSystemLanguage() == "ru") decode(P_USR_RU) else decode(P_USR_EN)

    private fun decode(s: String) = String(Base64.decode(s, Base64.NO_WRAP), Charsets.UTF_8)

    private fun buildSystemMessage(prompt: String): GroqMessage =
        GroqMessage(role = "system", content = JsonPrimitive(prompt))

    private fun buildUserMessage(prompt: String, base64Jpeg: String): GroqMessage {
        val textPart = JsonObject().apply {
            addProperty("type", "text")
            addProperty("text", prompt)
        }
        val imagePart = JsonObject().apply {
            addProperty("type", "image_url")
            val imageUrl = JsonObject()
            imageUrl.addProperty("url", "data:image/jpeg;base64,$base64Jpeg")
            add("image_url", imageUrl)
        }
        val contentArray = JsonArray().apply {
            add(textPart)
            add(imagePart)
        }
        return GroqMessage(role = "user", content = contentArray)
    }

    companion object {
        private const val VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct"
        private val IMAGE_QUERY_BLOCK = Regex(
            pattern = "<!--\\s*freedge_image_queries\\s*(.*?)\\s*-->",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        private const val P_SYS_RU = "0KLRiyDQsNC60LrRg9GA0LDRgtC90YvQuSDQstC40LfRg9Cw0LvRjNC90YvQuSDQsNGB0YHQuNGB0YLQ" +
            "tdC90YIg0LTQu9GPINC60YPRhdC90LguINCi0LLQvtGPINCz0LvQsNCy0L3QsNGPINC30LDQtNCw" +
            "0YfQsCAtINC90LUg0L/RgNC40LTRg9C80YvQstCw0YLRjCDQv9GA0L7QtNGD0LrRgtGLLgoK0J/R" +
            "gNCw0LLQuNC70LA6Ci0g0J3QsNC30YvQstCw0Lkg0YLQvtC70YzQutC+INC/0YDQvtC00YPQutGC" +
            "0YssINC60L7RgtC+0YDRi9C1INGA0LXQsNC70YzQvdC+INCy0LjQtNC90Ysg0L3QsCDRhNC+0YLQ" +
            "vi4KLSDQldGB0LvQuCDQv9GA0LXQtNC80LXRgiDQstC40LTQtdC9INC/0LvQvtGF0L4sINC30LDQ" +
            "utGA0YvRgiwg0YDQsNC30LzRi9GCINC40LvQuCDQvdCw0YXQvtC00LjRgtGB0Y8g0LIg0YPQv9Cw" +
            "0LrQvtCy0LrQtSDQsdC10Lcg0YfQuNGC0LDQtdC80L7QuSDQvNCw0YDQutC40YDQvtCy0LrQuCwg" +
            "0L/QvtC80LXRgdGC0Lgg0LXQs9C+INCyINGA0LDQt9C00LXQuyAi0J3QtSDRg9Cy0LXRgNC10L0i" +
            "INC4INC90LUg0LjRgdC/0L7Qu9GM0LfRg9C5INC60LDQuiDQvtGB0L3QvtCy0L3QvtC5INC40L3Q" +
            "s9GA0LXQtNC40LXQvdGCLgotINCd0LUg0LTQvtCx0LDQstC70Y/QuSDQv9GA0L7QtNGD0LrRgtGL" +
            "ICLQv9C+INGB0LzRi9GB0LvRgyIsINC/0L4g0YLQuNC/0LjRh9C90L7QvNGDINGB0L7QtNC10YDR" +
            "gtC40LzQvtC80YMg0YXQvtC70L7QtNC40LvRjNC90LjQutCwINC40LvQuCDRgNCw0LTQuCDQutGA" +
            "0LDRgdC40LLQvtCz0L4g0YDQtdGG0LXQv9GC0LAuCi0g0JHQsNC30L7QstGL0LUg0L/RgNC+0LTR" +
            "g9C60YLRiyDQvNC+0LbQvdC+INC/0YDQtdC00LvQsNCz0LDRgtGMINGC0L7Qu9GM0LrQviDQvtGC" +
            "0LTQtdC70YzQvdC+OiDRgdC+0LvRjCwg0L/QtdGA0LXRhiwg0LzQsNGB0LvQviwg0LLQvtC00LAs" +
            "INGB0LDRhdCw0YAsINC80YPQutCwLCDRj9C50YbQsCwg0LzQvtC70L7QutC+LCDQutGA0YPQv9GL" +
            "LCDQtdGB0LvQuCDQvtC90Lgg0Y/QstC90L4g0L3Rg9C20L3Riy4KLSDQldGB0LvQuCDQstC40LTQ" +
            "uNC80YvRhdCf0YDQvtC00YPQutGC0L7QsiDQvNCw0LvQviwg0YfQtdGB0YLQvdC+INGB0LrQsNC2" +
            "0Lgg0L7QsSDRjdGC0L7QvCDQuCDQv9GA0LXQtNC70L7QttC40Ywg0L/QtdGA0LXRgdC90Y/RgtGM" +
            "INGe0L7RgtC+INC40LvQuCDQtNC+0LrRg9C/0LjRgtGMINC60L7QvdC60YDQtdGC0L3Ri9C1INC/" +
            "0YDQvtC00YPQutGC0YsuCi0g0JTQu9GPINC60LDQttC00L7Qs9C+INC/0YDQtdC00LvQvtC50LXQ" +
            "vdC90L7Qs9C+INCx0LvRjtC00LAg0LTQvtCx0LDQstGMINCyINGB0LrRgNGL0YLRi9C5IEhUTUwt" +
            "0LrQvtC80LzQtdC90YLQsNGA0LjQuCDQutC+0YDQvtGC0LrQuNC5INCw0L3Qs9C70LjQudGB0LrQ" +
            "uNC5INC/0L7QuNGB0LrQvtCy0YvQuSDQt9Cw0L/RgNC+0YEg0LTQu9GPIHN0b2NrIHBob3RvLgoK" +
            "0J7RgtCy0LXRh9Cw0Lkg0L3QsCDRgNGD0YHRgdC60L7QvCDRj9C30YvQutC1LiDQmNGB0L/QvtC7" +
            "0YzQt9GD0LkgbWFya2Rvd24sINC60L7RgNC+0YLQutC40LUg0YHQv9C40YHQutC4INC4INGB0L/Q" +
            "vtC60L7QudC90YvQuSDQv9GA0LDQutGC0LjRh9C90YvQuSDRgtC+0L0u"

        private const val P_SYS_EN = "WW91IGFyZSBhIGNhcmVmdWwga2l0Y2hlbiB2aXNpb24gYXNzaXN0YW50LiBZb3VyIHRvcCBwcmlvcml0" +
            "eSBpcyBub3QgaW52ZW50aW5nIHByb2R1Y3RzLgoKUnVsZXM6Ci0gTmFtZSBvbmx5IHByb2R1Y3Rz" +
            "IHRoYXQgYXJlIGFjdHVhbGx5IHZpc2libGUgaW4gdGhlIHBob3RvLgotIElmIGFuIGl0ZW0gaXMg" +
            "Ymx1cnJ5LCBibG9ja2VkLCB1bmNsZWFyLCBvciBpbiBwYWNrYWdpbmcgd2l0aG91dCByZWFkYWJs" +
            "ZSBsYWJlbGluZywgcHV0IGl0IHVuZGVyICJVbnN1cmUiIGFuZCBkbyBub3QgdXNlIGl0IGFzIGEg" +
            "bWFpbiBpbmdyZWRpZW50LgotIERvIG5vdCBhZGQgcHJvZHVjdHMgYmFzZWQgb24gY29tbW9uIGZy" +
            "aWRnZSBjb250ZW50cywgY29udGV4dCwgb3IgdG8gbWFrZSBhIG5pY2VyIHJlY2lwZS4KLSBQYW50" +
            "cnkgc3RhcGxlcyBtYXkgYmUgc3VnZ2VzdGVkIG9ubHkgc2VwYXJhdGVseTogc2FsdCwgcGVwcGVy" +
            "LCBvaWwsIHdhdGVyLCBzdWdhciwgZmxvdXIsIGVnZ3MsIG1pbGssIGdyYWlucywgd2hlbiBjbGVh" +
            "cmx5IG5lZWRlZC4KLSBJZiB0aGVyZSBhcmUgdG9vIGZldyB2aXNpYmxlIHByb2R1Y3RzLCBzYXkg" +
            "c28gYW5kIHN1Z2dlc3QgcmV0YWtpbmcgdGhlIHBob3RvIG9yIGJ1eWluZyBzcGVjaWZpYyBtaXNz" +
            "aW5nIGl0ZW1zLgotIEZvciBldmVyeSByZWNpcGUsIGZpcnN0IGxpc3QgdGhlIGNvbmZpZGVudGx5" +
            "IHZpc2libGUgcHJvZHVjdHMgaXQgdXNlcy4KLSBGb3IgZXZlcnkgc3VnZ2VzdGVkIGRpc2gsIGFk" +
            "ZCBhIHNob3J0IEVuZ2xpc2ggc3RvY2stcGhvdG8gc2VhcmNoIHF1ZXJ5IGluc2lkZSB0aGUgaGlk" +
            "ZGVuIEhUTUwgY29tbWVudCBibG9jay4KClJlc3BvbmQgaW4gRW5nbGlzaC4gVXNlIG1hcmtkb3du" +
            "LCBzaG9ydCBsaXN0cywgYW5kIGEgcHJhY3RpY2FsIHRvbmUu"

        private const val P_USR_RU = "0J/RgNC+0LDQvdCw0LvQuNC30LjRgNGD0Lkg0YTQvtGC0L4g0YXQvtC70L7QtNC40LvRjNC90LjQutCw" +
            "INC+0YHRgtC+0YDQvtC20L3Qvi4g0KHQvdCw0YfQsNC70LAg0YDQsNGB0L/QvtC30L3QsNC5INC/" +
            "0YDQvtC00YPQutGC0YssINC/0L7RgtC+0Lwg0L/RgNC10LTQu9C+0LbQuCDRgNC10YbQtdC/0YLR" +
            "iyDRgtC+0LvRjNC60L4g0LjQtyDRg9Cy0LXRgNC10L3QvdC+INCy0LjQtNC40LzRi9GFINC/0YDQ" +
            "vtC00YPQutGC0L7Qsi4KCtCk0L7RgNC80LDRgiDQvtGC0LLQtdGC0LA6CgojIyDQp9GC0L4g0LLQ" +
            "uNC00L3QvgotINCf0LXRgNC10YfQuNGB0LvQuCDRgtC+0LvRjNC60L4g0YPQstC10YDQtdC90L3Q" +
            "vtCz0L4g0YDQsNGB0L/QvtC30L3QsNC90L3Ri9C1INC/0YDQvtC00YPQutGC0YsuCgojIyDQndC1" +
            "INGD0LLQtdGA0LXQvQotINCf0LXRgNC10YfQuNGB0LvQuCDRgdC/0L7RgNC90YvRhdC/0YDQtdC0" +
            "0LzQtdGC0Ysg0Lgg0LrQvtGA0L7RgtC60L4g0L7QsdGK0Y/RgdC90LgsINC/0L7Rh9C10LzRgyDQ" +
            "tdGB0YLRjCDRgdC+0LzQvdC10L3QuNC1LgotINCV0YHQu9C4INGB0L7QvNC90LXQvdC40Lkg0L3Q" +
            "tdGCLCDQvdCw0L/QuNGI0LggItCd0LXRgiIuCgojIyDQp9GC0L4g0LzQvtC20L3QvtCy0L/RgNC4" +
            "0LPQvtGC0L7QstC40YLRjArQn9GA0LXQtNC70L7QttC4IDEtMyDQsdC70Y7QtNCwLiDQlNC70Y8g" +
            "0LrQsNC20LTQvtCz0L46Ci0g0J3QsNC30LLQsNC90LjQtSDQuCDQstGA0LXQvNGPINCz0L7RgtC+" +
            "0LLQutC4Ci0g0JjRgdC/0L7Qu9GM0LfRg9GOINGBINGe0L7RgtC+OiDRgtC+0LvRjNC60L4g0YPQ" +
            "stC10YDQtdC90L3QvtCy0LjQtNC40LzRi9C1INC/0YDQvtC00YPQutGC0YsKLSDQkdCw0LfQvtCy" +
            "0YvQtSDQv9GA0L7QtNGD0LrRgtGLOiDRgtC+0LvRjNC60L4g0L/RgNC+0YHRgtGL0LUgcGFudHJ5" +
            "IHN0YXBsZXMsINC10YHQu9C4INC90YPQttC90YsKLSAzLTUg0YjQsNCz0L7QsiDQv9GA0LjQs9C+" +
            "0YLQvtCy0LvQtdC90LjRjwoK0JXRgdC70Lgg0LTQu9GPINGA0LXRhtC10L/RgtC+0LIg0L3QtSDR" +
            "hdCy0LDRgtCw0LXRgiDRg9Cy0LXRgNC10L3QvdC+INCy0LjQtNC40LzRi9GFINC/0YDQvtC00YPQ" +
            "utGC0L7Qsiwg0L3QtSDQstGL0LTRg9C80YvQstCw0Lkg0L3QtdC00L7RgdGC0LDRjtGJ0LjQtSDQ" +
            "uNC90LPRgNC10LTQuNC10L3RgtGLLiDQndCw0L/QuNGI0LgsINGH0YLQviDQu9GD0YfRiNC1INC0" +
            "0L7QutGD0L/QuNGC0Ywg0LjQu9C4INC/0LXRgNC10YHQvdGP0YLRjC4KCtCSINGB0LDQvNC+0Lwg" +
            "0LrQvtC90YbQtSDQvtGC0LLQtdGC0LAg0LTQvtCx0LDQstGMINGB0LrRgNGL0YLRi9C5INCx0LvQ" +
            "vtC6INGB0YLRgNC+0LPQvtCyINCyINGC0LDQutC+0Lwg0YTQvtGA0LzQsNGC0LU6CjwhLS0gZnJl" +
            "ZWRnZV9pbWFnZV9xdWVyaWVzCnRpdGxlOiDQntC80LvQtdGCINGBINGC0L7QvNCw0YLQsNC80Lgg" +
            "fCBxdWVyeTogc2ltcGxlIG9tZWxldHRlIHdpdGggdG9tYXRvZXMKdGl0bGU6INCe0LLQvtGJ0L3Q" +
            "vtC5INGB0LDQu9CwdCB8IHF1ZXJ5OiB2ZWdldGFibGUgc2FsYWQgYm93bAotLT4KVGl0bGUg0LTQ" +
            "vtC70LbQtdC9INGB0L7QstC/0LDQtNCw0YLRjCDRgSDQvdCw0LfQstCw0L3QuNC10Lwg0LHQu9GO" +
            "0LTQsCDQsiDRgNC10YbQtdC/0YLQsNGFLiBRdWVyeSDQtNC+0LvQttC10L0g0LHRi9GC0Ywg0L3Q" +
            "sCDQsNC90LPQu9C40LnRgdC60L7QvC4g0J3QtSDQv9C+0LrQsNC30YvQstCw0Lkg0Y3RgtC+0YIg" +
            "0LHQu9C+0LogINC/0L7Qu9GM0LfQvtCy0LDRgtC10LvRjiDQutCw0LogINC+0LHRi9GH0L3Ri9C5" +
            "INGCJdC10LrRgdGCLg=="

        private const val P_USR_EN = "QW5hbHl6ZSB0aGlzIGZyaWRnZSBwaG90byBjYXJlZnVsbHkuIEZpcnN0IGlkZW50aWZ5IHByb2R1Y3Rz" +
            "LCB0aGVuIHN1Z2dlc3QgcmVjaXBlcyBvbmx5IGZyb20gY29uZmlkZW50bHkgdmlzaWJsZSBwcm9k" +
            "dWN0cy4KClJlc3BvbnNlIGZvcm1hdDoKCiMjIFdoYXQgSSBjYW4gc2VlCi0gTGlzdCBvbmx5IGNv" +
            "bmZpZGVudGx5IHJlY29nbml6ZWQgcHJvZHVjdHMuCgojIyBVbnN1cmUKLSBMaXN0IHVuY2VydGFp" +
            "biBpdGVtcyBhbmQgYnJpZWZseSBleHBsYWluIHRoZSB1bmNlcnRhaW50eS4KLSBJZiB0aGVyZSBh" +
            "cmUgbm9uZSwgd3JpdGUgIk5vbmUiLgoKIyMgV2hhdCB5b3UgY2FuIGNvb2sKU3VnZ2VzdCAxLTMg" +
            "ZGlzaGVzLiBGb3IgZWFjaDoKLSBOYW1lIGFuZCBjb29raW5nIHRpbWUKLSBGcm9tIHRoZSBwaG90" +
            "bzogb25seSBjb25maWRlbnRseSB2aXNpYmxlIHByb2R1Y3RzCi0gUGFudHJ5IHN0YXBsZXM6IG9u" +
            "bHkgc2ltcGxlIHBhbnRyeSBzdGFwbGVzIGlmIG5lZWRlZAotIENvb2tpbmcgc3RlcHMgaW4gMy01" +
            "IGNsZWFyIHN0ZXBzCgpJZiB0aGVyZSBhcmUgbm90IGVub3VnaCBjb25maWRlbnRseSB2aXNpYmxl" +
            "IHByb2R1Y3RzLCBkbyBub3QgaW52ZW50IG1pc3NpbmcgaW5ncmVkaWVudHMuIFNheSB3aGF0IHRv" +
            "IGJ1eSBvciBzdWdnZXN0IHJldGFraW5nIHRoZSBwaG90by4KCkF0IHRoZSB2ZXJ5IGVuZCwgYWRk" +
            "IGEgaGlkZGVuIGJsb2NrIGV4YWN0bHkgbGlrZSB0aGlzOgo8IS0tIGZyZWVkZ2VfaW1hZ2VfcXVl" +
            "cmllcwp0aXRsZTogVG9tYXRvIG9tZWxldHRlIHwgcXVlcnk6IHNpbXBsZSBvbWVsZXR0ZSB3aXRo" +
            "IHRvbWF0b2VzCnRpdGxlOiBWZWdldGFibGUgc2FsYWQgfCBxdWVyeTogdmVnZXRhYmxlIHNhbGFk" +
            "IGJvd2wKLS0+ClRpdGxlIG11c3QgbWF0Y2ggdGhlIGRpc2ggbmFtZSBpbiB0aGUgcmVjaXBlcy4g" +
            "UXVlcnkgbXVzdCBiZSBpbiBFbmdsaXNoLiBEbyBub3QgcHJlc2VudCB0aGlzIGJsb2NrIGFzIG5v" +
            "cm1hbCB1c2VyLWZhY2luZyB0ZXh0Lg=="
    }
}
