package com.example.rec.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SummaryRequest(
    val transcript: String   // 👈 MUST be "transcript"
)

data class SummaryResponse(
    val title: String,
    val summary: String,
    val action_items: List<String>,
    val key_points: List<String>
)

interface SummaryApi {

    @POST("analyze") // 👈 NOT /summarize
    suspend fun analyze(
        @Body request: SummaryRequest
    ): Response<SummaryResponse>
}
