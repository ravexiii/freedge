package kg.freedge.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kg.freedge.AppContextHolder

fun buildDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.create {
        AppContextHolder.context.preferencesDataStoreFile("freedge_settings")
    }
