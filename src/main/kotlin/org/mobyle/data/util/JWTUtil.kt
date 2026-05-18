package org.mobyle.data.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mobyle.domain.model.JWTClaims
import org.mobyle.domain.model.User
import org.slf4j.LoggerFactory
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val log = LoggerFactory.getLogger("JWTUtil")

class JWTUtil(
    private val jwtSecret: String,
    val jwtExpirySeconds: Long = 3600,
    private val jwtIssuer: String = "moovie-backend"
) {

    fun generateToken(user: User): String {
        val now = System.currentTimeMillis() / 1000
        val expiresAt = now + jwtExpirySeconds

        val header = """{"alg":"HS256","typ":"JWT"}"""
        val avatarField = if (user.avatar != null) "\"avatar\":\"${user.avatar}\"," else ""
        val payload = """{
            "userId":"${user.id}",
            "email":"${user.email}",
            "username":"${user.username}",
            $avatarField
            "iat":$now,
            "exp":$expiresAt,
            "iss":"$jwtIssuer"
        }"""

        val headerEncoded = encodeBase64Url(header.toByteArray())
        val payloadEncoded = encodeBase64Url(payload.toByteArray())
        val message = "$headerEncoded.$payloadEncoded"

        val signature = hmacSha256(message, jwtSecret)
        val signatureEncoded = encodeBase64Url(signature)

        return "$message.$signatureEncoded"
    }

    fun validateToken(token: String): Result<JWTClaims> {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                return Result.failure(Exception("Invalid token format"))
            }

            val headerEncoded = parts[0]
            val payloadEncoded = parts[1]
            val signatureEncoded = parts[2]

            // Verify signature
            val message = "$headerEncoded.$payloadEncoded"
            val expectedSignature = encodeBase64Url(hmacSha256(message, jwtSecret))

            if (!constantTimeEquals(signatureEncoded, expectedSignature)) {
                return Result.failure(Exception("Invalid token signature"))
            }

            // Decode and parse payload
            val payloadJson = decodeBase64Url(payloadEncoded).decodeToString()
            val json = Json.parseToJsonElement(payloadJson).jsonObject

            val userId = json["userId"]?.jsonPrimitive?.content
                ?: return Result.failure(Exception("Missing userId claim"))
            val email = json["email"]?.jsonPrimitive?.content
                ?: return Result.failure(Exception("Missing email claim"))
            val username = json["username"]?.jsonPrimitive?.content
                ?: return Result.failure(Exception("Missing username claim"))
            val avatar = json["avatar"]?.jsonPrimitive?.content
            val iat = json["iat"]?.jsonPrimitive?.content?.toLongOrNull()
                ?: return Result.failure(Exception("Missing iat claim"))
            val exp = json["exp"]?.jsonPrimitive?.content?.toLongOrNull()
                ?: return Result.failure(Exception("Missing exp claim"))

            // Check expiration
            val now = System.currentTimeMillis() / 1000
            if (exp < now) {
                return Result.failure(Exception("Token expired"))
            }

            val claims = JWTClaims(
                userId = userId,
                email = email,
                username = username,
                avatar = avatar,
                iat = iat,
                exp = exp
            )

            Result.success(claims)
        } catch (e: Exception) {
            log.error("Token validation failed: ${e.message}")
            Result.failure(Exception("Invalid token: ${e.message}"))
        }
    }

    private fun hmacSha256(message: String, secret: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val key = SecretKeySpec(secret.toByteArray(), 0, secret.length, "HmacSHA256")
        mac.init(key)
        return mac.doFinal(message.toByteArray())
    }

    private fun encodeBase64Url(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun decodeBase64Url(encoded: String): ByteArray {
        val padded = encoded + "=".repeat((4 - encoded.length % 4) % 4)
        return Base64.getUrlDecoder().decode(padded)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
