package kg.freedge.core.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual class ImageCompressor actual constructor() {

    actual suspend fun compressForUpload(bytes: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        if (bytes.isEmpty()) return@withContext bytes

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext bytes

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE_PX)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: return@withContext bytes

        val matrix = Matrix().apply {
            val scale = scaleFactor(decoded.width, decoded.height, MAX_SIDE_PX)
            if (scale < 1f) postScale(scale, scale)
            val rotation = readExifRotation(bytes)
            if (rotation != 0) postRotate(rotation.toFloat())
        }

        val finalBitmap = if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
        }

        val out = ByteArrayOutputStream(bytes.size.coerceAtMost(256 * 1024))
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        finalBitmap.recycle()
        out.toByteArray()
    }

    private fun computeSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        val longSide = maxOf(width, height)
        // Keep halving while we'd still be >= 2× the target — leaves room for a clean scale pass.
        while (longSide / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    private fun scaleFactor(width: Int, height: Int, maxSide: Int): Float {
        val longSide = maxOf(width, height)
        return if (longSide > maxSide) maxSide.toFloat() / longSide.toFloat() else 1f
    }

    private fun readExifRotation(bytes: ByteArray): Int = try {
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (_: Exception) {
        0
    }
}
