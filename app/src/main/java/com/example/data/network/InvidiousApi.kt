package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class VideoThumbnail(
    val quality: String? = null,
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class SearchResultItem(
    val type: String? = null,
    val title: String? = null,
    val videoId: String? = null,
    val author: String? = null,
    val authorId: String? = null,
    val lengthSeconds: Int? = null,
    val videoThumbnails: List<VideoThumbnail>? = null
)

@JsonClass(generateAdapter = true)
data class FormatStream(
    val url: String? = null,
    val container: String? = null,
    val size: String? = null,
    val quality: String? = null
)

@JsonClass(generateAdapter = true)
data class AdaptiveFormat(
    val url: String? = null,
    val container: String? = null,
    val type: String? = null,
    val bitrate: String? = null
)

@JsonClass(generateAdapter = true)
data class VideoDetailsResponse(
    val title: String? = null,
    val videoId: String? = null,
    val formatStreams: List<FormatStream>? = null,
    val adaptiveFormats: List<AdaptiveFormat>? = null
)

interface InvidiousApi {
    @GET("api/v1/search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("type") type: String = "video"
    ): List<SearchResultItem>

    @GET("api/v1/videos/{videoId}")
    suspend fun getVideoDetails(
        @Path("videoId") videoId: String
    ): VideoDetailsResponse
}
