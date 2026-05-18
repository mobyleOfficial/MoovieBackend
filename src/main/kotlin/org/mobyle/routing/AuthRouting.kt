package org.mobyle.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.mobyle.di.injection
import org.mobyle.domain.model.AuthToken
import org.mobyle.domain.model.OAuthCallbackRequest
import org.mobyle.domain.model.User
import org.mobyle.domain.usecase.auth.ProcessOAuthCallback
import org.mobyle.domain.usecase.auth.RefreshToken
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AuthRouting")

fun Route.getAuthRouting() {
    val processOAuthCallback by injection<ProcessOAuthCallback>()
    val refreshTokenUseCase by injection<RefreshToken>()

    post("/api/v1/auth/oauth/callback") {
        try {
            val request = call.receive<OAuthCallbackRequest>()

            if (request.code.isBlank() || request.state.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("invalid_request", "Missing required parameters: code, state")
                )
                return@post
            }

            val result = processOAuthCallback(request)

            if (result.isSuccess) {
                val authToken = result.getOrThrow()
                call.respond(
                    HttpStatusCode.OK,
                    AuthTokenResponse(
                        accessToken = authToken.accessToken,
                        tokenType = authToken.tokenType,
                        expiresIn = authToken.expiresIn,
                        refreshToken = authToken.refreshToken,
                        user = authToken.user
                    )
                )
                log.info("OAuth callback processed successfully for user ${authToken.user.id}")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                val (statusCode, errorCode) = when {
                    error.contains("invalid_state") -> HttpStatusCode.BadRequest to "invalid_state"
                    error.contains("invalid_code") -> HttpStatusCode.BadRequest to "invalid_code"
                    error.contains("provider_error") -> HttpStatusCode.BadGateway to "provider_error"
                    error.contains("user_creation_failed") -> HttpStatusCode.InternalServerError to "user_creation_failed"
                    else -> HttpStatusCode.InternalServerError to "internal_error"
                }

                log.error("OAuth callback failed: $error")
                call.respond(
                    statusCode,
                    ErrorResponse(errorCode, error)
                )
            }
        } catch (e: Exception) {
            log.error("OAuth callback request parsing error: ${e.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("invalid_request", "Invalid request body: ${e.message}")
            )
        }
    }

    post("/api/v1/auth/refresh") {
        try {
            val request = call.receive<RefreshTokenRequest>()

            if (request.refreshToken.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("invalid_request", "Refresh token is required")
                )
                return@post
            }

            val result = refreshTokenUseCase(request.refreshToken)

            if (result.isSuccess) {
                val authToken = result.getOrThrow()
                call.respond(
                    HttpStatusCode.OK,
                    RefreshTokenResponse(
                        accessToken = authToken.accessToken,
                        tokenType = authToken.tokenType,
                        expiresIn = authToken.expiresIn
                    )
                )
                log.info("Token refreshed successfully for user ${authToken.user.id}")
            } else {
                log.error("Token refresh failed: ${result.exceptionOrNull()?.message}")
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("invalid_refresh_token", "Refresh token expired or invalid")
                )
            }
        } catch (e: Exception) {
            log.error("Token refresh request parsing error: ${e.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("invalid_request", "Invalid request body: ${e.message}")
            )
        }
    }
}

@Serializable
data class AuthTokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val refreshToken: String? = null,
    val user: User
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)
