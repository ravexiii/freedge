package kg.freedge.shared.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal expect fun createFreedgeHttpClient(): HttpClient

internal val freedgeJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

internal fun HttpClientConfig<*>.configureFreedgeHttpClient() {
    expectSuccess = true
    install(ContentNegotiation) {
        json(freedgeJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 20_000
        requestTimeoutMillis = 60_000
        socketTimeoutMillis = 60_000
    }
}
