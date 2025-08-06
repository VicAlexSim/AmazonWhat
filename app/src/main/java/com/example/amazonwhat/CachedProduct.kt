// CachedProduct.kt
package com.example.amazonwhat

// Add Gson annotations if you use it for serialization directly,
// or ensure your API response parsing already handles this structure.
import com.google.gson.annotations.SerializedName

data class CachedProduct(
    @SerializedName("productId") // Matches Kroger API field if you directly serialize parts of it
    val productId: String,
    @SerializedName("description") // Or simply "name"
    val name: String,
    // You'll need to extract the price carefully from the API's structure.
    // Assuming you can get a simple Double for the regular price.
    val price: Double,
    // Assuming you pick one representative image URL.
    val imageUrl: String?,
    val category: String // The search term/category this product was found under
)