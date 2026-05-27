package kg.freedge.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")

class OnboardingPreferences(private val dataStore: DataStore<Preferences>) {

    val isCompleted: Flow<Boolean> = dataStore.data
        .map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun setCompleted() {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }
}
