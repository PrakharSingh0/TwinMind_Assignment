package com.example.twinmind.data.remote

import com.example.rec.network.SummaryApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // 🔹 Whisper (Hugging Face)
    private const val WHISPER_BASE_URL = "https://prakharsingh0-wishpermodel.hf.space/"

    // 🔹 Gemini (Render)
    private const val GEMINI_BASE_URL = "https://twinmind-backend-8kly.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 🔥 Client for Gemini API with standard timeouts
    private val geminiOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // 🔥 Client for Whisper API with a longer timeout for transcription
    private val whisperOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()


    // ---------- Whisper Retrofit ----------
    private val whisperRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(WHISPER_BASE_URL)
            .client(whisperOkHttpClient) // 🔥 USE WHISPER CLIENT
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val transcriptionApi: TranscriptionApi by lazy {
        whisperRetrofit.create(TranscriptionApi::class.java)
    }

    // ---------- Gemini Retrofit ----------
    private val geminiRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(geminiOkHttpClient) // 🔥 USE GEMINI CLIENT
            .addConverterFactory(GsonConverterFactory.create()) // JSON only
            .build()
    }

    val summaryApi: SummaryApi by lazy {
        geminiRetrofit.create(SummaryApi::class.java)
    }
}
