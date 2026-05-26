package kg.freedge.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun buildDataStore(): DataStore<Preferences> {
    val docsPath = NSFileManager.defaultManager.URLForDirectory(
        NSDocumentDirectory, NSUserDomainMask, null, true, null
    )!!.path!!
    return PreferenceDataStoreFactory.createWithPath {
        "$docsPath/freedge_settings.preferences_pb".toPath()
    }
}
