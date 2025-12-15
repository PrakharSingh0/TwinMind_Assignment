package com.example.twinmind.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface TranscriptionApi {

    @Multipart
    @POST("transcribe")
    suspend fun transcribe(
        @Part audio: MultipartBody.Part
    ): Response<TranscriptionResponse>
}
