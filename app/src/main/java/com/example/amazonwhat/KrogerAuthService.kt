// KrogerAuthService.kt
package com.example.amazonwhat // Or your app's package name

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface KrogerAuthService {

    /**
     * Fetches an OAuth2 access token using the client_credentials grant type.
     *
     * @param authorization Basic authentication header: "Basic <base64(client_id:client_secret)>".
     * @param grantType Should be "client_credentials".
     * @param scope The requested scope, e.g., "product.compact".
     * @return A Response containing the access token.
     */
    @FormUrlEncoded // Important: This indicates the request body is form-urlencoded
    @POST("v1/connect/oauth2/token") // The token endpoint from Kroger docs
    suspend fun getAccessToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("scope") scope: String = "product.compact" // Your required scope
    ): Response<KrogerTokenResponse>
}
