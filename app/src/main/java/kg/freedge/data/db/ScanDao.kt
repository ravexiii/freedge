package kg.freedge.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert
    suspend fun insert(scan: ScanEntity): Long

    @Query("SELECT * FROM scans ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun getById(id: Long): ScanEntity?

    @Delete
    suspend fun delete(scan: ScanEntity)
}
