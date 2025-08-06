// KrogerApiClient.kt
package com.example.amazonwhat // Or your app's package name

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File // For Cache directory
import java.util.concurrent.TimeUnit

object KrogerApiClient {

    // Base URL for the Kroger API
    private const val BASE_URL = "https://api.kroger.com/" // Ensure this ends with a '/'

    // Create a logging interceptor for OkHttp for debugging network calls
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Logs request and response lines and their respective headers and bodies (if present).
        // Use Level.BASIC for less verbosity in production.
    }

    private var okHttpClientInstance: OkHttpClient? = null
    private var retrofitInstance: Retrofit? = null

    // Call this from your Application class or MainActivity's onCreate
    fun initialize(context: Context) {
        if (okHttpClientInstance == null) {
            val applicationContext = context.applicationContext
            val cacheSize = (10 * 1024 * 1024).toLong() // 10 MB Cache
            // Use a specific directory within the app's cache folder for OkHttp
            val httpCacheDirectory = File(applicationContext.cacheDir, "http-cache")
            val cache = Cache(httpCacheDirectory, cacheSize)

            okHttpClientInstance = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .cache(cache) // Enable HTTP caching
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    private val client: OkHttpClient
        get() {
            if (okHttpClientInstance == null) {
                throw IllegalStateException("KrogerApiClient must be initialized with context before use. Call KrogerApiClient.initialize(context).")
            }
            return okHttpClientInstance!!
        }

    private val retrofit: Retrofit
        get() {
            if (retrofitInstance == null) {
                retrofitInstance = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client) // Use the OkHttpClient with caching
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofitInstance!!
        }

    val krogerApiService: KrogerApiService by lazy {
        retrofit.create(KrogerApiService::class.java)
    }
}