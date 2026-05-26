package kg.freedge.data.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun buildRoomDatabase(): FreedgeDatabase {
    val docsPath = NSFileManager.defaultManager.URLForDirectory(
        NSDocumentDirectory, NSUserDomainMask, null, true, null
    )!!.path!!
    return Room.databaseBuilder<FreedgeDatabase>(name = "$docsPath/freedge_kmp.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
