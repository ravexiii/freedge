package kg.freedge.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

actual class ImageCompressor actual constructor() {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun compressForUpload(bytes: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            if (bytes.isEmpty()) return@withContext bytes

            val nsData = bytes.toNSData() ?: return@withContext bytes
            // UIImage(data:) honours EXIF orientation, so subsequent drawInRect produces
            // an upright bitmap and we can emit JPEG without an orientation tag.
            val source = UIImage.imageWithData(nsData) ?: return@withContext bytes

            val scaled = source.scaledToFit(MAX_SIDE_PX.toDouble())
            val jpeg = UIImageJPEGRepresentation(scaled, JPEG_QUALITY / 100.0)
                ?: return@withContext bytes

            jpeg.toByteArray() ?: bytes
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun UIImage.scaledToFit(maxSide: Double): UIImage {
        val (width, height) = size.useContents { width to height }
        if (width <= 0.0 || height <= 0.0) return this

        val longSide = maxOf(width, height)
        if (longSide <= maxSide) return this

        val scale = maxSide / longSide
        val targetW = width * scale
        val targetH = height * scale
        // scale = 1.0 → output pixel dimensions match targetSize (no @2x/@3x multiplier),
        // keeping JPEG payload size predictable.
        val format = UIGraphicsImageRendererFormat().apply {
            this.scale = 1.0
            this.opaque = true
        }
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetW, targetH), format = format)
        return renderer.imageWithActions { _ ->
            drawInRect(CGRectMake(0.0, 0.0, targetW, targetH))
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ByteArray.toNSData(): NSData? {
        if (isEmpty()) return null
        return usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray? {
        val length = length.toInt()
        if (length <= 0) return null
        val result = ByteArray(length)
        result.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), bytes, this.length)
        }
        return result
    }
}
