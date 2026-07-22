package org.mobyle.domain.usecase.auth

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.mobyle.domain.model.AuthToken
import org.mobyle.domain.model.User
import org.mobyle.domain.repository.AuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginUserTest {

    private val authRepository = mockk<AuthRepository>()
    private val loginUser = LoginUser(authRepository)

    private val testUser = User(
        id = "user-123",
        email = "test@example.com",
        username = "testuser",
        avatar = null,
        createdAt = "2026-07-22T10:00:00Z"
    )

    @Test
    fun `new user returns success with isNewUser true`() = runTest {
        val authToken = AuthToken(
            accessToken = "jwt-token",
            tokenType = "Bearer",
            expiresIn = 3600,
            user = testUser,
            isNewUser = true
        )
        coEvery { authRepository.loginUser("new@example.com", "securePassword123") } returns Result.success(authToken)

        val result = loginUser("new@example.com", "securePassword123")

        assertTrue(result.isSuccess)
        val token = result.getOrThrow()
        assertTrue(token.isNewUser)
        assertEquals("jwt-token", token.accessToken)
    }

    @Test
    fun `existing user with correct password returns success`() = runTest {
        val authToken = AuthToken(
            accessToken = "jwt-token",
            tokenType = "Bearer",
            expiresIn = 3600,
            user = testUser,
            isNewUser = false
        )
        coEvery { authRepository.loginUser("test@example.com", "correctPassword1") } returns Result.success(authToken)

        val result = loginUser("test@example.com", "correctPassword1")

        assertTrue(result.isSuccess)
        val token = result.getOrThrow()
        assertTrue(!token.isNewUser)
    }

    @Test
    fun `wrong password returns failure with invalid_credentials`() = runTest {
        coEvery { authRepository.loginUser("test@example.com", "wrongPassword1") } returns
            Result.failure(Exception("invalid_credentials"))

        val result = loginUser("test@example.com", "wrongPassword1")

        assertTrue(result.isFailure)
        assertEquals("invalid_credentials", result.exceptionOrNull()?.message)
    }

    @Test
    fun `password too short returns failure with invalid_password_length`() = runTest {
        coEvery { authRepository.loginUser("test@example.com", "short") } returns
            Result.failure(Exception("invalid_password_length"))

        val result = loginUser("test@example.com", "short")

        assertTrue(result.isFailure)
        assertEquals("invalid_password_length", result.exceptionOrNull()?.message)
    }

    @Test
    fun `delegates to auth repository correctly`() = runTest {
        val authToken = AuthToken(
            accessToken = "token",
            tokenType = "Bearer",
            expiresIn = 3600,
            user = testUser,
            isNewUser = true
        )
        coEvery { authRepository.loginUser("user@test.com", "password123!") } returns Result.success(authToken)

        val result = loginUser("user@test.com", "password123!")

        assertTrue(result.isSuccess)
        assertEquals("token", result.getOrThrow().accessToken)
    }
}
