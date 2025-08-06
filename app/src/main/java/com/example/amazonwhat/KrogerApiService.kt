// KrogerApiService.kt
package com.example.amazonwhat // Or your app's package name

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface KrogerApiService {

    /**
     * Searches for products based on various filter criteria.
     * At least one of filter.term, filter.brand, or filter.productId is required.
     *
     * @param authToken The OAuth2 bearer token.
     * @param term Search term (e.g., "milk").
     * @param locationId Location ID to get location-specific data (price, inventory).
     * @param productId Product ID(s), comma-separated for multiple.
     * @param brand Brand name(s), pipe-separated for multiple.
     * @param fulfillment Fulfillment types (ais, csp, dth, sth), comma-separated.
     * @param start Pagination: number of products to skip.
     * @param limit Pagination: number of products to return (max 50).
     * @return A Response containing the product search results.
     */
    @GET("v1/products")
    suspend fun searchProducts(
        @Header("Authorization") authToken: String,
        @Query("filter.term") term: String? = null,
        @Query("filter.locationId") locationId: String? = null,
        @Query("filter.productId") productId: String? = null,
        @Query("filter.brand") brand: String? = null,
        @Query("filter.fulfillment") fulfillment: String? = null,
        @Query("filter.start") start: Int? = null,
        @Query("filter.limit") limit: Int? = null // Default is 10, max 50 as per docs
    ): Response<KrogerProductSearchResponse>

    /**
     * Fetches details for a specific product by its ID or UPC.
     *
     * @param authToken The OAuth2 bearer token.
     * @param id The product ID or UPC.
     * @param locationId Location ID to get location-specific data (price, inventory).
     * @return A Response containing the product details.
     */
    @GET("v1/products/{id}")
    suspend fun getProductDetails(
        @Header("Authorization") authToken: String,
        @Path("id") id: String,
        @Query("filter.locationId") locationId: String? = null
    ): Response<KrogerProductDetailResponse>
}
