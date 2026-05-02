package kg.freedge.shared.data.network

import kotlinx.serialization.Serializable

@Serializable
internal data class PexelsSearchResponse(
    val photos: List<PexelsPhoto>? = null
)

@Serializable
internal data class PexelsPhoto(
    val id: Long,
    val url: String,
    val photographer: String,
    val src: PexelsPhotoSrc
)

@Serializable
internal data class PexelsPhotoSrc(
    val medium: String? = null,
    val large: String? = null,
    val landscape: String? = null
)
