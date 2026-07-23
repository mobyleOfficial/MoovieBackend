package org.mobyle.domain.usecase.auth

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.mobyle.domain.repository.AuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogoutUserTest {

    private val authRepository = mockk<AuthRepository>()
    private val logoutUser = LogoutUser(authRepository)

    @Test
    fun `valid token is revoked successfully`() = runTest {
        coEvery { authRepository.logoutUser("valid-jwt-token") } returns Result.success(Unit)

        val result = logoutUser("valid-jwt-token")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invalid token returns failure`() = runTest {
        coEvery { authRepository.logoutUser("invalid-token") } returns
            Result.failure(Exception("invalid_token"))

        val result = logoutUser("invalid-token")

        assertTrue(result.isFailure)
        assertEquals("invalid_token", result.exceptionOrNull()?.message)
    }

    @Test
    fun `already revoked token returns failure`() = runTest {
        coEvery { authRepository.logoutUser("revoked-token") } returns
            Result.failure(Exception("token_revoked"))

        val result = logoutUser("revoked-token")

        assertTrue(result.isFailure)
        assertEquals("token_revoked", result.exceptionOrNull()?.message)
    }
}
