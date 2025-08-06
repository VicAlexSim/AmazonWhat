// KrogerAuthClient.kt
package com.example.amazonwhat // Or your app's package name

import android.util.Base64 // For Base64 encoding
import android.util.Log     // For logging
import com.example.amazonwhat.BuildConfig // <--- IMPORT THIS
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object KrogerAuthClient {

    // Base URL for the Kroger API
    private const val AUTH_BASE_URL = "https://api.kroger.com/"

    // Access secrets from BuildConfig
    private val CLIENT_ID = BuildConfig.KROGER_CLIENT_ID
    private val CLIENT_SECRET = BuildConfig.KROGER_CLIENT_SECRET

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(AUTH_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: KrogerAuthService by lazy {
        retrofit.create(KrogerAuthService::class.java)
    }

    /**
     * Creates the Basic Authentication header value.
     * Format: "Basic <base64_encoded_string_of_client_id:client_secret>"
     */
    fun getBasicAuthHeaderValue(): String {
        // Add a check to ensure keys are not empty (especially if default values from BuildConfig are used)
        if (CLIENT_ID.isEmpty() || CLIENT_SECRET.isEmpty()) {
            Log.e("KrogerAuthClient", "Client ID or Secret is empty. " +
                    "Check local.properties and ensure it's synced via BuildConfig.")
            // You might want to throw an exception or handle this more gracefully
            // Returning a non-functional header to potentially avoid crashing on Base64 encoding an empty string,
            // but the API call will fail.
            return "Basic Og==" // "Basic :" (empty credentials)
        }
        val credentials = "$CLIENT_ID:$CLIENT_SECRET"
        return "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
    }
}
