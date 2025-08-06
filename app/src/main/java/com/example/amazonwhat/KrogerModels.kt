// KrogerModels.kt
package com.example.amazonwhat // Or your app's package name

import com.google.gson.annotations.SerializedName

// --- Top-Level Response Structures ---

/**
 * Represents the response for a product search.
 * Contains a list of products and metadata.
 */
data class KrogerProductSearchResponse(
    @SerializedName("data") val data: List<ProductModel>?,
    @SerializedName("meta") val meta: Meta?
)

/**
 * Represents the response for fetching details of a single product.
 * Contains a single product and metadata.
 */
data class KrogerProductDetailResponse(
    @SerializedName("data") val data: ProductModel?,
    @SerializedName("meta") val meta: Meta?
)

// --- Core Product Model (as per products.productModel in docs) ---

/**
 * Represents a single product from the Kroger catalog.
 */
data class ProductModel(
    @SerializedName("productId") val productId: String, // e.g., "0001111041700"
    @SerializedName("upc") val upc: String?,             // Universal Product Code, e.g., "0001111041700"
    @SerializedName("aisleLocations") val aisleLocations: List<AisleLocation>?,
    @SerializedName("brand") val brand: String?,         // e.g., "Kroger"
    @SerializedName("categories") val categories: List<String>?, // e.g., ["Dairy"]
    @SerializedName("countryOrigin") val countryOrigin: String?, // e.g., "United States"
    @SerializedName("description") val description: String?, // e.g., "Kroger 2% Reduced Fat Milk"
    @SerializedName("items") val items: List<ItemEntry>?, // Array of item-specific details (price, size, etc.)
    @SerializedName("itemInformation") val itemInformation: ItemInformation?, // Physical dimensions, etc.
    @SerializedName("temperature") val temperature: TemperatureIndicator?, // Storage temperature info
    @SerializedName("images") val images: List<ProductImagePerspective>?,
    @SerializedName("productPageURI") val productPageURI: String? // URI to the product page on Kroger's site
)

// --- Supporting Data Classes ---

/**
 * Contains metadata about the API response, like pagination.
 */
data class Meta(
    @SerializedName("pagination") val pagination: Pagination?,
    @SerializedName("warnings") val warnings: List<String>? // e.g., "filter.locationId is required to display pricing information"
)

/**
 * Details for paginating through results.
 */
data class Pagination(
    @SerializedName("total") val total: Int?,    // Total number of results
    @SerializedName("start") val start: Int?,    // The starting index of the current results
    @SerializedName("limit") val limit: Int?     // The number of results returned in this page
)

/**
 * Describes the location of an item within a store aisle.
 * Requires filter.locationId to be populated.
 */
data class AisleLocation(
    @SerializedName("bayNumber") val bayNumber: String?,
    @SerializedName("description") val description: String?, // e.g., "Aisle Back Left"
    @SerializedName("number") val number: String?,           // e.g., "17" (Aisle number)
    @SerializedName("numberOfFacings") val numberOfFacings: String?,
    @SerializedName("sequenceNumber") val sequenceNumber: String?,
    @SerializedName("side") val side: String?,               // e.g., "L" (Left side of aisle)
    @SerializedName("shelfNumber") val shelfNumber: String?,
    @SerializedName("shelfPositionInBay") val shelfPositionInBay: String?
)

/**
 * Represents an entry in the "items" array of a ProductModel.
 * Contains price, size, fulfillment options, and inventory status for a specific version/package of the product.
 */
data class ItemEntry(
    @SerializedName("itemId") val itemId: String?, // Specific ID for this item variation (e.g., specific size)
    @SerializedName("favorite") val favorite: Boolean?, // If the item is marked as a favorite by a user (may not be relevant for client_credentials)
    @SerializedName("fulfillment") val fulfillment: FulfillmentAvailability?,
    @SerializedName("price") val price: PriceInfo?, // Regular and promo price, requires filter.locationId
    @SerializedName("nationalPrice") val nationalPrice: PriceInfo?, // National regular and promo price, requires filter.locationId
    @SerializedName("size") val size: String?, // e.g., "1 GAL", "12 fl oz"
    @SerializedName("soldBy") val soldBy: String?, // e.g., "UNIT", "WEIGHT"
    @SerializedName("inventory") val inventory: InventoryStatus? // Stock level, requires filter.locationId
)

/**
 * Indicates the availability of different fulfillment methods for an item.
 * Requires filter.locationId.
 */
data class FulfillmentAvailability(
    @SerializedName("curbside") val curbside: Boolean?,   // Available for curbside pickup
    @SerializedName("delivery") val delivery: Boolean?,   // Available for delivery
    @SerializedName("instore") val instore: Boolean?,    // Sold in the store (not necessarily in stock)
    @SerializedName("shiptohome") val shiptohome: Boolean? // Available to be shipped to home
)

/**
 * Contains regular and promotional price for an item.
 * Requires filter.locationId.
 */
data class PriceInfo(
    @SerializedName("regular") val regular: Double?, // The regular price
    @SerializedName("promo") val promo: Double?      // The promotional price (can be 0.0 or null if no promo)
)

/**
 * Provides the inventory stock level of an item.
 * Requires filter.locationId. Property is omitted if unavailable.
 */
data class InventoryStatus(
    // The API doc says "Returns the stockLevel of the item. This property is omitted when unavailable".
    // So, the InventoryStatus object itself might be null, or stockLevel within it might be null.
    @SerializedName("stockLevel") val stockLevel: String? // "HIGH", "LOW", "TEMPORARILY_OUT_OF_STOCK"
)

/**
 * Information about the physical characteristics of the item.
 * Structure not fully detailed in the provided documentation snippet.
 */
data class ItemInformation(
    @SerializedName("depth") val depth: String?,
    @SerializedName("height") val height: String?,
    @SerializedName("width") val width: String?
    // Other fields like 'extendedDescription', 'shortDescription' might exist.
    // Inspect live API response if more detail is needed.
)

/**
 * Information about the item's temperature requirements.
 * Structure not fully detailed in the provided documentation snippet.
 */
data class TemperatureIndicator(
    @SerializedName("heatSensitive") val heatSensitive: Boolean?,
    @SerializedName("indicator") val indicator: String?, // e.g., "Refrigerated", "Frozen", "Shelf Stable"
    @SerializedName("storageTips") val storageTips: String? // e.g., "KEEP REFRIGERATED"
)

/**
 * Represents an image of the product from a specific perspective.
 * Images have different sizes available.
 */
data class ProductImagePerspective(
    @SerializedName("perspective") val perspective: String, // e.g., "front", "back", "nutrition"
    @SerializedName("featured") val featured: Boolean?, // Indicates if this is the primary image for the perspective
    @SerializedName("sizes") val sizes: List<ImageSizeDetail>? // List of available image URLs for different sizes
)

/**
 * Details of a specific size of a product image, including its URL.
 */
data class ImageSizeDetail(
    @SerializedName("size") val size: String, // e.g., "thumbnail", "small", "medium", "large", "xlarge"
    @SerializedName("url") val url: String   // The URL of the image at this size
)

