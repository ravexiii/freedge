package kg.freedge.analytics

import android.content.Context
import android.util.Log

// TODO: когда добавишь google-services.json — раскомментируй Firebase и удали Log-заглушку
// import android.os.Bundle
// import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsManager(@Suppress("UNUSED_PARAMETER") context: Context) {

    fun logScanStarted() = log("scan_started")
    fun logScanSuccess() = log("scan_success")
    fun logScanError(errorType: String) = log("scan_error", "error_type=$errorType")
    fun logHistoryOpened() = log("history_opened")
    fun logScanViewed() = log("scan_viewed")
    fun logOnboardingCompleted() = log("onboarding_completed")

    private fun log(event: String, params: String = "") {
        Log.d("Analytics", if (params.isEmpty()) event else "$event [$params]")
    }
}
