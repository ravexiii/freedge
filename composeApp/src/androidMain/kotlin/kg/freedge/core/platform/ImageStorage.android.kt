package kg.freedge.core.platform

import kg.freedge.AppContextHolder
import java.io.File

actual class ImageStorage actual constructor() {

    private val dir: File
        get() = File(AppContextHolder.context.filesDir, "scans").also { it.mkdirs() }

    actual fun save(fileName: String, bytes: ByteArray): String {
        val file = dir.resolve(fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual fun load(fileName: String): ByteArray? =
        runCatching { dir.resolve(fileName).readBytes() }.getOrNull()

    actual fun delete(fileName: String) {
        dir.resolve(fileName).delete()
    }
}
