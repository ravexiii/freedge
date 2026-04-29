package kg.freedge.ui.main

import android.app.Application
import kg.freedge.app.R
import kg.freedge.data.repo.FreedgeErrorCode
import kg.freedge.data.repo.FreedgeException

object MainErrorHandler {

    fun userMessage(app: Application, error: Throwable): String {
        return when ((error as? FreedgeException)?.code) {
            FreedgeErrorCode.MissingGroqApiKey -> app.getString(R.string.error_missing_api_key)
            FreedgeErrorCode.EmptyResponse -> app.getString(R.string.error_empty_response)
            FreedgeErrorCode.ApiAuth -> app.getString(R.string.error_api_auth)
            FreedgeErrorCode.ApiRateLimited -> app.getString(R.string.error_api_rate_limited)
            FreedgeErrorCode.ApiServer -> app.getString(R.string.error_api_server)
            FreedgeErrorCode.Network -> app.getString(R.string.error_no_internet)
            FreedgeErrorCode.Unknown, null -> error.message ?: app.getString(R.string.error_generic)
        }
    }

    fun analyticsKey(error: Throwable): String {
        return (error as? FreedgeException)?.code?.name ?: error.javaClass.simpleName
    }
}
