// KrogerAuthModels.kt
package com.example.amazonwhat // Or your app's package name

import com.google.gson.annotations.SerializedName

data class KrogerTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: Int, // Duration in seconds
    @SerializedName("token_type") val tokenType: String, // Usually "bearer"
    @SerializedName("scope") val scope: String? // Optional: The scopes granted
)