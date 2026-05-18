package org.mobyle.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.mobyle.domain.model.JWTClaims
import org.mobyle.domain.usecase.auth.ValidateToken
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("JWTAuth")

class JWTPrincipal(val claims: JWTClaims) : java.security.Principal {
    override fun getName(): String = claims.userId
}

suspend fun ApplicationCall.authenticateJWT(validateToken: ValidateToken): JWTPrincipal? {
    return try {
        val authHeader = request.headers["Authorization"] ?: run {
            log.debug("Missing Authorization header")
            respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "missing_token", "message" to "Authorization header missing")
            )
            return null
        }

        if (!authHeader.startsWith("Bearer ")) {
            log.debug("Invalid Authorization header format")
            respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "invalid_token_format", "message" to "Authorization must be Bearer token")
            )
            return null
        }

        val token = authHeader.substring("Bearer ".length)
        val result = validateToken(token)

        if (result.isFailure) {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            log.debug("JWT validation failed: $error")
            respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "invalid_token", "message" to error)
            )
            return null
        }

        JWTPrincipal(result.getOrThrow())
    } catch (e: Exception) {
        log.error("JWT authentication error: ${e.message}")
        respond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to "authentication_error", "message" to "Invalid token")
        )
        null
    }
}
