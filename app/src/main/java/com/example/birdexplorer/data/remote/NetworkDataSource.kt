package com.example.birdexplorer.data.remote

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

// Singleton to access the API through retrofit builder
object NetworkDataSource {
    private val json = Json { ignoreUnknownKeys = true } // API may return more fields so we ignore de Unknown ones

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    // explain
    val service: INatApiService = retrofit.create(INatApiService::class.java)
}