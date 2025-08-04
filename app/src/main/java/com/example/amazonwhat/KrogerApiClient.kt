// KrogerApiClient.kt
package com.example.amazonwhat // Or your app's package name

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object KrogerApiClient {

    // Base URL for the Kroger API
    private const val BASE_URL = "https://api.kroger.com/" // Ensure this ends with a '/'

    // Create a logging interceptor for OkHttp for debugging network calls
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Logs request and response lines and their respective headers and bodies (if present).
        // Use Level.BASIC for less verbosity in production.
    }

    // Configure OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // Add the logging interceptor
        .connectTimeout(30, TimeUnit.SECONDS) // Set connection timeout
        .readTimeout(30, TimeUnit.SECONDS)    // Set read timeout
        .writeTimeout(30, TimeUnit.SECONDS)   // Set write timeout
        .build()

    // Configure Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient) // Use the custom OkHttpClient
        .addConverterFactory(GsonConverterFactory.create()) // Use Gson for JSON parsing
        .build()

    // Lazily create the KrogerApiService instance
    val krogerApiService: KrogerApiService by lazy {
        retrofit.create(KrogerApiService::class.java)
    }
}