// MyApplication.kt
package com.example.amazonwhat // Your package name

import android.app.Application
import android.util.Log
import com.example.amazonwhat.KrogerAuthClient // Your KrogerAuthClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApplication : Application() {

    // Create a custom CoroutineScope that will live as long as the application
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Example location ID. CRITICAL: Find a valid one for your testing.
    private val KROGER_LOCATION_ID_FOR_PREFETCH = "01400943" // Ensure this is a valid one

    override fun onCreate() {
        super.onCreate()

        // Initialize things that need application context
        KrogerApiClient.initialize(applicationContext) // Initialize your API client
        ProductCacheManager.initialize(applicationContext)

        // Trigger background prefetch
        prefetchInitialData()
    }

    private fun prefetchInitialData() {
        applicationScope.launch {
            Log.d("MyApplication", "Checking cache and potentially prefetching data...")
            val cachedProducts = ProductCacheManager.loadProductsFromCache()
            if (cachedProducts.isEmpty()) { // Only prefetch if cache is empty
                Log.i("MyApplication", "Cache is empty. Starting background prefetch...")

                // Token fetching logic - similar to MainActivity's ensureValidToken
                // This needs to be self-contained or use a shared auth manager
                var token: String? = null
                try {
                    // Critical: Ensure KrogerAuthClient and its authService are usable here.
                    // This might require KrogerAuthClient to be a singleton or initialized statically.
                    if (BuildConfig.KROGER_CLIENT_ID.isEmpty() || BuildConfig.KROGER_CLIENT_SECRET.isEmpty()) {
                        Log.e("MyApplication", "Kroger Client ID or Secret is empty for prefetch.")
                        return@launch
                    }
                    val authHeader = KrogerAuthClient.getBasicAuthHeaderValue() // Assuming static access or singleton
                    val response = KrogerAuthClient.authService.getAccessToken(authorization = authHeader)

                    if (response.isSuccessful && response.body() != null) {
                        token = "Bearer ${response.body()!!.accessToken}"
                        Log.i("MyApplication", "Prefetch: Successfully obtained token.")
                    } else {
                        Log.e("MyApplication", "Prefetch: Failed to get token. Code: ${response.code()}, Error: ${response.errorBody()?.string()}")
                        return@launch // Can't prefetch without a token
                    }
                } catch (e: Exception) {
                    Log.e("MyApplication", "Prefetch: Exception during token fetch", e)
                    return@launch
                }

                if (token != null) {
                    val success = ProductCacheManager.bulkFetchAndCacheProducts(
                        locationId = KROGER_LOCATION_ID_FOR_PREFETCH,
                        tokenProvider = { token } // Provide the fetched token
                        // No onProgress needed for silent background fetch, or log it
                    )
                    if (success) {
                        Log.i("MyApplication", "Background prefetch completed successfully.")
                    } else {
                        Log.w("MyApplication", "Background prefetch finished, but no new items were cached or it failed.")
                    }
                }
            } else {
                Log.i("MyApplication", "Cache already populated with ${cachedProducts.size} items. No prefetch needed now.")
            }
        }
    }
}
