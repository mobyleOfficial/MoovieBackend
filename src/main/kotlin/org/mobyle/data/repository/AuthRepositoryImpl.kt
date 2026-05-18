package org.mobyle.data.repository

import org.mobyle.data.local.oauth.OAuthStateDataSource
import org.mobyle.data.remote.oauth.OAuthDataSource
import org.mobyle.data.util.JWTUtil
import org.mobyle.domain.model.AuthToken
import org.mobyle.domain.model.JWTClaims
import org.mobyle.domain.model.OAuthCallbackRequest
import org.mobyle.domain.model.User
import org.mobyle.domain.repository.AuthRepository
import org.mobyle.domain.repository.UserRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AuthRepositoryImpl")

class AuthRepositoryImpl(
    private val oauthDataSource: OAuthDataSource,
    private val oauthStateDataSource: OAuthStateDataSource,
    private val userRepository: UserRepository,
    private val jwtUtil: JWTUtil
) : AuthRepository {

    override suspend fun processOAuthCallback(request: OAuthCallbackRequest): Result<AuthToken> {
        return try {
            // Validate state parameter (CSRF protection)
            val stateValid = oauthStateDataSource.validateAndRemoveState(request.state).getOrNull() ?: false
            if (!stateValid) {
                log.warn("Invalid OAuth state parameter")
                return Result.failure(Exception("invalid_state"))
            }

            // Exchange code for token
            val tokenResult = oauthDataSource.exchangeCodeForToken(request.code)
            if (tokenResult.isFailure) {
                log.error("OAuth token exchange failed: ${tokenResult.exceptionOrNull()?.message}")
                return Result.failure(Exception("invalid_code"))
            }

            val oauthToken = tokenResult.getOrThrow()

            // Get user info from OAuth provider
            val userInfoResult = oauthDataSource.getUserInfo(oauthToken.accessToken)
            if (userInfoResult.isFailure) {
                log.error("Failed to get OAuth user info: ${userInfoResult.exceptionOrNull()?.message}")
                return Result.failure(Exception("provider_error"))
            }

            var user = userInfoResult.getOrThrow()

            // Create or update user in local storage
            val savedUserResult = userRepository.createOrUpdateUser(user)
            if (savedUserResult.isFailure) {
                log.error("Failed to save user: ${savedUserResult.exceptionOrNull()?.message}")
                return Result.failure(Exception("user_creation_failed"))
            }

            user = savedUserResult.getOrThrow()

            // Generate JWT token
            val accessToken = jwtUtil.generateToken(user)

            val authToken = AuthToken(
                accessToken = accessToken,
                tokenType = "Bearer",
                expiresIn = jwtUtil.jwtExpirySeconds,
                refreshToken = oauthToken.refreshToken,
                user = user
            )

            Result.success(authToken)
        } catch (e: Exception) {
            log.error("OAuth callback processing failed: ${e.message}")
            Result.failure(Exception("internal_error"))
        }
    }

    override suspend fun validateToken(token: String): Result<JWTClaims> {
        return jwtUtil.validateToken(token)
    }

    override suspend fun refreshToken(refreshToken: String): Result<AuthToken> {
        // Phase 2 implementation
        return Result.failure(Exception("not_implemented"))
    }

    override suspend fun getUserById(userId: String): Result<User> {
        val userResult = userRepository.getUserById(userId)
        return if (userResult.isSuccess && userResult.getOrNull() != null) {
            Result.success(userResult.getOrNull()!!)
        } else {
            Result.failure(Exception("user_not_found"))
        }
    }
}
