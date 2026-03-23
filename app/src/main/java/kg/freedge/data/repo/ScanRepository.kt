package kg.freedge.data.repo

import kg.freedge.data.db.FreedgeDatabase
import kg.freedge.data.db.ScanEntity
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val db: FreedgeDatabase) {

    suspend fun saveScan(imageBytes: ByteArray, result: String): Long {
        return db.scanDao().insert(
            ScanEntity(imageBytes = imageBytes, result = result)
        )
    }

    fun getAllScans(): Flow<List<ScanEntity>> = db.scanDao().getAll()

    suspend fun getScanById(id: Long): ScanEntity? = db.scanDao().getById(id)

    suspend fun deleteScan(scan: ScanEntity) = db.scanDao().delete(scan)
}
