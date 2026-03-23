package kg.freedge.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kg.freedge.analytics.AnalyticsManager
import kg.freedge.data.preferences.OnboardingPreferences
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = OnboardingPreferences(application)
    private val analytics = AnalyticsManager(application)

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.setCompleted()
            analytics.logOnboardingCompleted()
            onDone()
        }
    }
}
