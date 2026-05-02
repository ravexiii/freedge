package kg.freedge.shared.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createFreedgeHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        configureFreedgeHttpClient()
    }
