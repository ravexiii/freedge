package kg.freedge.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun compressForUpload(imageBytes: ByteArray, maxSide: Int = 1024, quality: Int = 80): ByteArray {
        val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return imageBytes

        val scale = minOf(
            maxSide.toFloat() / original.width,
            maxSide.toFloat() / original.height,
            1f
        )

        val out = ByteArrayOutputStream()

        if (scale >= 1f) {
            // Уже достаточно маленькое — только перекодируем с нужным quality
            original.compress(Bitmap.CompressFormat.JPEG, quality, out)
            original.recycle()
        } else {
            val scaled = Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            )
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            scaled.recycle()
            original.recycle()
        }

        return out.toByteArray()
    }
}
