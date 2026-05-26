package kg.freedge.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kg.freedge.core.currentTimeMillis

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageFileName: String,
    val result: String,
    val createdAt: Long = currentTimeMillis()
)
