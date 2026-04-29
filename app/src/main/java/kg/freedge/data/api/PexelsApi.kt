package kg.freedge.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PexelsApi {
    @GET("v1/search")
    suspend fun searchPhotos(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
        @Query("orientation") orientation: String = "landscape"
    ): PexelsSearchResponse
}

data class PexelsSearchResponse(
    val photos: List<PexelsPhoto>?
)

data class PexelsPhoto(
    val id: Long,
    val url: String,
    val photographer: String,
    val src: PexelsPhotoSrc
)

data class PexelsPhotoSrc(
    val medium: String?,
    val large: String?,
    val landscape: String?
)
