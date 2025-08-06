// ProductCacheManager.kt
package com.example.amazonwhat

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object ProductCacheManager {

    private const val CACHE_FILE_NAME = "product_cache.json"
    private var applicationContext: Context? = null
    private val gson = Gson()

    // Common search terms/categories for pre-fetching
    // TODO: Expand this list with 20+ relevant categories for your game
    val commonSearchTerms = listOf(
        "milk", "bread", "eggs", "cheese", "yogurt", "apples", "bananas", "orange juice",
        "chicken breast", "ground beef", "rice", "pasta", "cereal", "coffee", "tea",
        "soda", "chips", "cookies", "ice cream", "shampoo", "soap", "toothpaste",
        "laundry detergent", "dish soap", "paper towels"
        // Add more diverse terms to get a good variety of products
    )

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        // It's good practice to ensure KrogerApiClient is also initialized if ProductCacheManager depends on it.
        // Assuming KrogerApiClient.initialize is called in your Application class or before this.
    }

    private fun ensureInitialized() {
        if (applicationContext == null) {
            throw IllegalStateException("ProductCacheManager must be initialized with context. Call ProductCacheManager.initialize(context).")
        }
    }

    private fun getCacheFile(): File {
        ensureInitialized()
        return File(applicationContext!!.filesDir, CACHE_FILE_NAME)
    }

    suspend fun saveProductsToCache(products: List<CachedProduct>) {
        ensureInitialized()
        withContext(Dispatchers.IO) {
            try {
                val jsonString = gson.toJson(products)
                getCacheFile().writeText(jsonString)
                Log.d("ProductCacheManager", "Successfully saved ${products.size} products to cache.")
            } catch (e: IOException) {
                Log.e("ProductCacheManager", "Error saving products to cache", e)
            }
        }
    }

    suspend fun loadProductsFromCache(): List<CachedProduct> {
        ensureInitialized()
        return withContext(Dispatchers.IO) {
            val cacheFile = getCacheFile()
            if (!cacheFile.exists()) {
                Log.d("ProductCacheManager", "Cache file does not exist. Returning empty list.")
                return@withContext emptyList()
            }
            try {
                val jsonString = cacheFile.readText()
                val type = object : TypeToken<List<CachedProduct>>() {}.type
                val products: List<CachedProduct> = gson.fromJson(jsonString, type) ?: emptyList()
                Log.d("ProductCacheManager", "Successfully loaded ${products.size} products from cache.")
                products
            } catch (e: Exception) {
                Log.e("ProductCacheManager", "Error loading products from cache", e)
                emptyList()
            }
        }
    }

    suspend fun clearCache() {
        ensureInitialized()
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = getCacheFile()
                if (cacheFile.exists()) {
                    if (cacheFile.delete()) {
                        Log.d("ProductCacheManager", "Cache cleared successfully.")
                    } else {
                        Log.w("ProductCacheManager", "Failed to delete cache file.")
                    }
                } else {
                    Log.d("ProductCacheManager", "Cache file does not exist. Nothing to clear.")
                }
            } catch (e: IOException) {
                Log.e("ProductCacheManager", "Error clearing cache", e)
            }
        }
    }

    /**
     * Fetches products in bulk from the Kroger API for common search terms
     * and saves them to the local cache.
     *
     * @param termsToFetch A list of search terms to fetch. Defaults to commonSearchTerms.
     * @param itemsPerTerm Number of items to try fetching for each term. Max 50 (Kroger API limit).
     * @param locationId The Kroger location ID for pricing and availability.
     * @param tokenProvider A suspend function that returns the Bearer token string or null.
     * @param onProgress Lambda to report progress (currentTermIndex, totalTerms, term, itemsFetchedThisTerm)
     * @return True if fetching resulted in some products being cached, false otherwise.
     */
    suspend fun bulkFetchAndCacheProducts(
        termsToFetch: List<String> = commonSearchTerms,
        itemsPerTerm: Int = 5,
        locationId: String?,
        tokenProvider: suspend () -> String?, // MODIFIED: Lambda to provide the token
        onProgress: (termBeingFetched: String, currentTermIndex: Int, totalTerms: Int, itemsFetchedThisTerm: Int) -> Unit = { _, _, _, _ -> }
    ): Boolean {
        ensureInitialized()
        // KrogerApiClient should be initialized by now.

        val allFetchedProducts = mutableListOf<CachedProduct>()
        var atLeastOneTermSucceeded = false

        Log.i("ProductCacheManager", "Starting bulk fetch for ${termsToFetch.size} terms. Location: $locationId")

        if (locationId.isNullOrBlank()) {
            Log.e("ProductCacheManager", "Location ID is null or blank. Cannot perform bulk fetch.")
            return false
        }

        termsToFetch.forEachIndexed { index, term ->
            val authToken = tokenProvider() // Get token for each term (or it could be cached by the provider)

            if (authToken.isNullOrBlank()) {
                Log.e("ProductCacheManager", "Auth token is missing or blank for term '$term'. Skipping this term.")
                onProgress(term, index, termsToFetch.size, -1) // -1 indicates skipped due to token
                return@forEachIndexed // Skip this term and continue with the next
            }

            Log.d("ProductCacheManager", "Fetching term (${index + 1}/${termsToFetch.size}): '$term'")
            var productsFetchedThisTerm = 0
            try {
                val response = KrogerApiClient.krogerApiService.searchProducts(
                    authToken = authToken,
                    term = term,
                    locationId = locationId,
                    limit = itemsPerTerm.coerceAtMost(50)
                )

                if (response.isSuccessful) {
                    val productDataList = response.body()?.data
                    if (!productDataList.isNullOrEmpty()) {
                        val productsForTerm = productDataList.mapNotNull { product ->
                            val priceInfo = product.items?.firstOrNull()?.price
                            // Prefer regular price, fallback to promo. Ensure it's not null and > 0.
                            val validPrice = listOfNotNull(priceInfo?.regular, priceInfo?.promo)
                                .firstOrNull { it > 0.0 }

                            val itemName = product.description
                            val itemId = product.productId

                            val frontImage = product.images?.find { it.perspective == "front" }
                            val imageUrl = frontImage?.sizes?.find { it.size == "large" }?.url
                                ?: frontImage?.sizes?.find { it.size == "medium" }?.url
                                ?: frontImage?.sizes?.firstOrNull()?.url
                                ?: product.images?.firstOrNull()?.sizes?.firstOrNull { it.url.isNotBlank() }?.url

                            if (itemId != null && itemName != null && validPrice != null) {
                                CachedProduct(
                                    productId = itemId,
                                    name = itemName,
                                    price = String.format("%.2f", validPrice).toDouble(),
                                    imageUrl = imageUrl, // Can be null
                                    category = term
                                )
                            } else {
                                Log.w("ProductCacheManager", "Skipping product (ID: $itemId, Name: $itemName, Price: $validPrice) due to missing essential data for term '$term'.")
                                null
                            }
                        }
                        allFetchedProducts.addAll(productsForTerm)
                        productsFetchedThisTerm = productsForTerm.size
                        atLeastOneTermSucceeded = true // Mark success if at least one item from one term is fetched
                        Log.d("ProductCacheManager", "Fetched $productsFetchedThisTerm products for term: '$term'")
                    } else {
                        Log.w("ProductCacheManager", "No product data in successful response for term: '$term'")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ProductCacheManager", "API error for term '$term': ${response.code()} - ${response.message()}. Body: $errorBody")
                }
            } catch (e: Exception) {
                Log.e("ProductCacheManager", "Exception during fetch for term '$term': ${e.message}", e)
            }
            onProgress(term, index, termsToFetch.size, productsFetchedThisTerm)
            // kotlinx.coroutines.delay(100) // Optional small delay
        }

        if (allFetchedProducts.isNotEmpty()) {
            val distinctProducts = allFetchedProducts.distinctBy { it.productId }
            saveProductsToCache(distinctProducts) // Replace existing cache
            Log.i("ProductCacheManager", "Bulk fetch complete. Total distinct products cached: ${distinctProducts.size}")
            return true // Successfully cached some products
        } else {
            Log.w("ProductCacheManager", "Bulk fetch completed but resulted in no products being cached. This could be due to API errors for all terms, no items matching criteria, or token issues.")
            if (!atLeastOneTermSucceeded && termsToFetch.isNotEmpty()) {
                // If no term even seemed to succeed (e.g. all token errors, or all API errors before mapping)
                // it might imply a broader issue. Deciding to clear cache here is optional.
                // For now, let's not clear if the existing cache might still be valid.
                Log.w("ProductCacheManager", "No terms yielded valid product data during fetch.")
            }
            return false // No new products were cached
        }
    }

    suspend fun getRandomProductsFromCache(count: Int): List<CachedProduct> {
        ensureInitialized() // Added ensureInitialized call
        val cachedProducts = loadProductsFromCache()
        return if (cachedProducts.isEmpty()) {
            emptyList()
        } else if (cachedProducts.size <= count) {
            cachedProducts.shuffled() // Shuffle even if returning all, for variety if called again
        } else {
            cachedProducts.shuffled().take(count)
        }
    }
}
