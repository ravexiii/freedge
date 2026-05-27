package kg.freedge.data.db

import androidx.room.Database
import androidx.room.ConstructedBy
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [ScanEntity::class], version = 1, exportSchema = false)
@ConstructedBy(FreedgeDatabaseConstructor::class)
abstract class FreedgeDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FreedgeDatabaseConstructor : RoomDatabaseConstructor<FreedgeDatabase> {
    override fun initialize(): FreedgeDatabase
}
