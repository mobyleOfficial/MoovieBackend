package org.mobyle.data.remote.oauth

import org.mobyle.domain.model.User

interface OAuthDataSource {
    suspend fun exchangeCodeForToken(code: String): Result<OAuthTokenResponse>
    suspend fun getUserInfo(accessToken: String): Result<User>
}

data class OAuthTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshToken: String? = null,
    val scope: String? = null
)
