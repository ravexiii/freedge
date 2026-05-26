package kg.freedge.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kg.freedge.AppContextHolder

fun buildRoomDatabase(): FreedgeDatabase =
    Room.databaseBuilder(
        AppContextHolder.context,
        FreedgeDatabase::class.java,
        "freedge_kmp.db"
    ).build()
