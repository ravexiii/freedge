package kg.freedge.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageBytes: ByteArray,
    val result: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ScanEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
