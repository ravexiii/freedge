package kg.freedge.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.freedge.data.preferences.OnboardingPreferences
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val prefs: OnboardingPreferences
) : ViewModel() {

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.setCompleted()
            onDone()
        }
    }
}
