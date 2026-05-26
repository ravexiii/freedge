package kg.freedge.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScanEntity::class], version = 1, exportSchema = false)
abstract class FreedgeDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
}
