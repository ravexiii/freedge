package kg.freedge.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL
import platform.posix.memcpy

actual class ImageStorage actual constructor() {

    private val scansDir: String
        get() {
            val docs = NSFileManager.defaultManager.URLForDirectory(
                NSDocumentDirectory, NSUserDomainMask, null, true, null
            )!!.path!!
            val dir = "$docs/scans"
            NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
            return dir
        }

    @OptIn(ExperimentalForeignApi::class)
    actual fun save(fileName: String, bytes: ByteArray): String {
        val path = "$scansDir/$fileName"
        val data = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        }
        data.writeToURL(NSURL.fileURLWithPath(path), atomically = true)
        return path
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun load(fileName: String): ByteArray? {
        val path = "$scansDir/$fileName"
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        if (data.length == 0UL) return null
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        return bytes
    }

    actual fun delete(fileName: String) {
        NSFileManager.defaultManager.removeItemAtPath("$scansDir/$fileName", error = null)
    }
}
