package kg.freedge.core.platform

/**
 * Downscales and re-encodes a captured JPEG so it can be sent to the vision API
 * without blowing past message-size limits or saturating cellular bandwidth.
 *
 * Implementations must:
 *  - honour the source EXIF orientation (the result has no rotation metadata to apply),
 *  - clamp the longer side to [MAX_SIDE_PX],
 *  - re-encode as JPEG at roughly [JPEG_QUALITY] quality,
 *  - never throw on decode failure — return the original bytes instead.
 */
expect class ImageCompressor() {
    suspend fun compressForUpload(bytes: ByteArray): ByteArray
}

internal const val MAX_SIDE_PX = 1024
internal const val JPEG_QUALITY = 80
