package org.mobyle.data.remote.oauth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.Parameters
import kotlinx.serialization.Serializable
import org.mobyle.domain.model.User
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger("OAuthDataSourceImpl")

class OAuthDataSourceImpl(
    private val httpClient: HttpClient,
    private val oauthClientId: String,
    private val oauthClientSecret: String,
    private val oauthProviderUrl: String,
    private val oauthRedirectUri: String
) : OAuthDataSource {

    override suspend fun exchangeCodeForToken(code: String): Result<OAuthTokenResponse> {
        return try {
            val tokenEndpoint = "$oauthProviderUrl/token"

            val response = httpClient.submitForm(
                url = tokenEndpoint,
                formParameters = Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("client_id", oauthClientId)
                    append("client_secret", oauthClientSecret)
                    append("redirect_uri", oauthRedirectUri)
                }
            )

            val tokenResponse: OAuthTokenResponseDto = response.body()

            Result.success(
                OAuthTokenResponse(
                    accessToken = tokenResponse.access_token,
                    tokenType = tokenResponse.token_type ?: "Bearer",
                    expiresIn = tokenResponse.expires_in ?: 3600,
                    refreshToken = tokenResponse.refresh_token,
                    scope = tokenResponse.scope
                )
            )
        } catch (e: Exception) {
            log.error("OAuth token exchange failed: ${e.message}")
            Result.failure(Exception("Invalid OAuth code or provider error: ${e.message}"))
        }
    }

    override suspend fun getUserInfo(accessToken: String): Result<User> {
        return try {
            val userInfoEndpoint = "$oauthProviderUrl/userinfo"
            val response = httpClient.get(userInfoEndpoint) {
                headers.append("Authorization", "Bearer $accessToken")
            }

            val userInfo: OAuthUserInfoDto = response.body()

            val user = User(
                id = userInfo.sub ?: UUID.randomUUID().toString(),
                email = userInfo.email ?: "",
                username = userInfo.name ?: userInfo.email?.substringBefore("@") ?: "user",
                avatar = userInfo.picture,
                createdAt = System.currentTimeMillis().toString()
            )

            Result.success(user)
        } catch (e: Exception) {
            log.error("OAuth user info retrieval failed: ${e.message}")
            Result.failure(Exception("Failed to retrieve user info: ${e.message}"))
        }
    }
}

@Serializable
data class OAuthTokenResponseDto(
    val access_token: String,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val refresh_token: String? = null,
    val scope: String? = null
)

@Serializable
data class OAuthUserInfoDto(
    val sub: String? = null,
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null
)
