package kg.freedge.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class OnboardingPreferences(private val context: Context) {

    private val key = booleanPreferencesKey("onboarding_completed")

    val isCompleted: Flow<Boolean> = context.dataStore.data
        .map { it[key] ?: false }

    suspend fun setCompleted() {
        context.dataStore.edit { it[key] = true }
    }
}
