package kg.freedge.data.repo

import android.content.Context
import kg.freedge.data.db.FreedgeDatabase
import kg.freedge.data.db.ScanEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class ScanRepository(private val db: FreedgeDatabase, private val context: Context) {

    suspend fun saveScan(imageBytes: ByteArray, result: String): Long {
        val fileName = "scan_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, "scans").apply { mkdirs() }.resolve(fileName)
        file.writeBytes(imageBytes)
        return db.scanDao().insert(ScanEntity(imagePath = file.absolutePath, result = result))
    }

    fun getAllScans(): Flow<List<ScanEntity>> = db.scanDao().getAll()

    suspend fun getScanById(id: Long): ScanEntity? = db.scanDao().getById(id)

    suspend fun deleteScan(scan: ScanEntity) {
        File(scan.imagePath).delete()
        db.scanDao().delete(scan)
    }

    fun loadImage(path: String): ByteArray? = try {
        File(path).readBytes()
    } catch (_: Exception) {
        null
    }
}
