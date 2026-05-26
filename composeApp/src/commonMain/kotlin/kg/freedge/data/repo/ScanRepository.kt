package kg.freedge.data.repo

import kg.freedge.core.currentTimeMillis
import kg.freedge.core.platform.ImageStorage
import kg.freedge.data.db.FreedgeDatabase
import kg.freedge.data.db.ScanEntity
import kotlinx.coroutines.flow.Flow

class ScanRepository(
    private val db: FreedgeDatabase,
    private val imageStorage: ImageStorage
) {

    suspend fun saveScan(imageBytes: ByteArray, result: String): Long {
        val fileName = "scan_${currentTimeMillis()}.jpg"
        imageStorage.save(fileName, imageBytes)
        return db.scanDao().insert(ScanEntity(imageFileName = fileName, result = result))
    }

    fun getAllScans(): Flow<List<ScanEntity>> = db.scanDao().getAll()

    suspend fun getScanById(id: Long): ScanEntity? = db.scanDao().getById(id)

    suspend fun deleteScan(scan: ScanEntity) {
        imageStorage.delete(scan.imageFileName)
        db.scanDao().delete(scan)
    }

    fun loadScanImage(scan: ScanEntity): ByteArray? = imageStorage.load(scan.imageFileName)
}
