package com.pukikiko.funny.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class VideoModel(
    val id: Int,
    val filename: String,
    val thumbs_up: Int,
    val thumbs_down: Int
)

data class VoteRequest(
    val action: String // "up" or "down"
)

data class UploadResponse(
    val message: String?,
    val error: String?
)

interface FunnyApi {
    @GET("/api/video/next")
    suspend fun getNextVideo(
        @Query("watched") watched: String,
        @Query("mode") mode: String = "algorithm"
    ): VideoModel

    @POST("/api/video/{id}/vote")
    suspend fun voteVideo(
        @Path("id") id: Int,
        @Body request: VoteRequest
    ): VideoModel

    // Uploads land in the moderation queue, not the public feed.
    @Multipart
    @POST("/upload")
    suspend fun uploadVideo(
        @Part video: MultipartBody.Part
    ): Response<UploadResponse>
}

object RetrofitClient {
    private var currentUrl: String? = null
    private var currentApi: FunnyApi? = null

    fun getApi(baseUrl: String): FunnyApi {
        if (currentUrl == baseUrl && currentApi != null) {
            return currentApi!!
        }
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val api = Retrofit.Builder()
            .baseUrl(safeUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FunnyApi::class.java)
        currentUrl = baseUrl
        currentApi = api
        return api
    }
}
