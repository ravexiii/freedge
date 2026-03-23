package kg.freedge.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScanEntity::class], version = 1, exportSchema = false)
abstract class FreedgeDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: FreedgeDatabase? = null

        fun getInstance(context: Context): FreedgeDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FreedgeDatabase::class.java,
                    "freedge_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
