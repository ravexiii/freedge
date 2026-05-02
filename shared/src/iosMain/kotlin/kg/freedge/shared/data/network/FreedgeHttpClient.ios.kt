package kg.freedge.shared.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createFreedgeHttpClient(): HttpClient =
    HttpClient(Darwin) {
        configureFreedgeHttpClient()
    }
