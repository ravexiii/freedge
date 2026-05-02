package kg.freedge.shared.data.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import kg.freedge.shared.FreedgeErrorCode
import kg.freedge.shared.FreedgeException
import kotlinx.serialization.SerializationException

internal object FreedgeNetworkErrorMapper {
    fun map(error: Throwable): FreedgeException =
        when (error) {
            is ClientRequestException -> {
                val status = error.response.status.value
                val code = when (status) {
                    401, 403 -> FreedgeErrorCode.ApiAuth
                    429 -> FreedgeErrorCode.ApiRateLimited
                    else -> FreedgeErrorCode.Unknown
                }
                FreedgeException(code, "api_error_$status", error)
            }
            is ServerResponseException -> {
                val status = error.response.status.value
                FreedgeException(FreedgeErrorCode.ApiServer, "api_error_$status", error)
            }
            is ResponseException -> {
                val status = error.response.status.value
                FreedgeException(FreedgeErrorCode.Unknown, "api_error_$status", error)
            }
            is SerializationException -> FreedgeException(
                FreedgeErrorCode.Unknown,
                error.message ?: "serialization_error",
                error
            )
            else -> FreedgeException(
                FreedgeErrorCode.Network,
                error.message ?: "network_error",
                error
            )
        }
}
