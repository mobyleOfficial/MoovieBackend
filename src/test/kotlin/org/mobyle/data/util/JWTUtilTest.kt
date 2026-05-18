package org.mobyle.data.util

import org.mobyle.domain.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JWTUtilTest {
    private val jwtUtil = JWTUtil(
        jwtSecret = "test-secret-key-with-minimum-32-characters-long",
        jwtExpirySeconds = 3600
    )

    @Test
    fun testGenerateTokenCreatesValidJWT() {
        val user = User(
            id = "user123",
            email = "test@example.com",
            username = "testuser",
            avatar = "https://example.com/avatar.jpg",
            createdAt = "2026-05-18T10:00:00Z"
        )

        val token = jwtUtil.generateToken(user)

        assertNotNull(token)
        assertEquals(3, token.split(".").size)
    }

    @Test
    fun testValidateTokenExtractsClaimsCorrectly() {
        val user = User(
            id = "user123",
            email = "test@example.com",
            username = "testuser",
            avatar = "https://example.com/avatar.jpg",
            createdAt = "2026-05-18T10:00:00Z"
        )

        val token = jwtUtil.generateToken(user)
        val result = jwtUtil.validateToken(token)

        assert(result.isSuccess)
        val claims = result.getOrThrow()
        assertEquals("user123", claims.userId)
        assertEquals("test@example.com", claims.email)
        assertEquals("testuser", claims.username)
        assertEquals("https://example.com/avatar.jpg", claims.avatar)
    }

    @Test
    fun testValidateTokenRejectsMalformedToken() {
        val result = jwtUtil.validateToken("not.a.valid.token")
        assert(result.isFailure)
    }

    @Test
    fun testValidateTokenRejectsInvalidSignature() {
        val user = User(
            id = "user123",
            email = "test@example.com",
            username = "testuser",
            avatar = null,
            createdAt = "2026-05-18T10:00:00Z"
        )

        val token = jwtUtil.generateToken(user)
        val parts = token.split(".")
        val tamperedToken = "${parts[0]}.${parts[1]}.invalidsignature"

        val result = jwtUtil.validateToken(tamperedToken)
        assert(result.isFailure)
    }

    @Test
    fun testValidateTokenRejectsExpiredToken() {
        val expiredJwtUtil = JWTUtil(
            jwtSecret = "test-secret-key-with-minimum-32-characters-long",
            jwtExpirySeconds = -10 // Already expired
        )

        val user = User(
            id = "user123",
            email = "test@example.com",
            username = "testuser",
            avatar = null,
            createdAt = "2026-05-18T10:00:00Z"
        )

        val token = expiredJwtUtil.generateToken(user)
        val result = jwtUtil.validateToken(token)

        assert(result.isFailure)
    }

    @Test
    fun testGenerateTokenWithNullAvatar() {
        val user = User(
            id = "user456",
            email = "test2@example.com",
            username = "testuser2",
            avatar = null,
            createdAt = "2026-05-18T10:00:00Z"
        )

        val token = jwtUtil.generateToken(user)
        val result = jwtUtil.validateToken(token)

        assert(result.isSuccess)
        val claims = result.getOrThrow()
        assertEquals(null, claims.avatar)
    }
}
