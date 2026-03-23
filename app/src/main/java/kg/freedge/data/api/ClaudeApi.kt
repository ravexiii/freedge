package kg.freedge.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ClaudeApi {

    @Headers("Content-Type: application/json", "anthropic-version: 2023-06-01")
    @POST("v1/messages")
    suspend fun analyze(
        @Header("x-api-key") apiKey: String,
        @Body request: ClaudeRequest
    ): ClaudeResponse
}