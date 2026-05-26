package kg.freedge.data.repo

import kg.freedge.core.currentTimeMillis
import kg.freedge.core.platform.ImageStorage
import kg.freedge.data.db.FreedgeDatabase
import kg.freedge.data.db.ScanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScanRepository(
    private val db: FreedgeDatabase,
    private val imageStorage: ImageStorage
) {

    suspend fun saveScan(imageBytes: ByteArray, result: String): Long {
        val fileName = "scan_${currentTimeMillis()}.jpg"
        withContext(Dispatchers.Default) { imageStorage.save(fileName, imageBytes) }
        return try {
            db.scanDao().insert(ScanEntity(imageFileName = fileName, result = result))
        } catch (e: Throwable) {
            runCatching { imageStorage.delete(fileName) }
            throw e
        }
    }

    fun getAllScans(): Flow<List<ScanEntity>> = db.scanDao().getAll()

    suspend fun getScanById(id: Long): ScanEntity? = db.scanDao().getById(id)

    suspend fun deleteScan(scan: ScanEntity) {
        db.scanDao().delete(scan)
        withContext(Dispatchers.Default) { imageStorage.delete(scan.imageFileName) }
    }

    suspend fun loadScanImage(scan: ScanEntity): ByteArray? =
        withContext(Dispatchers.Default) { imageStorage.load(scan.imageFileName) }
}
