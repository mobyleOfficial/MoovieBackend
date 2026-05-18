package org.mobyle.domain.usecase.auth

import org.mobyle.domain.model.AuthToken
import org.mobyle.domain.repository.AuthRepository

class RefreshToken(private val authRepository: AuthRepository) {
    suspend operator fun invoke(refreshToken: String): Result<AuthToken> {
        return authRepository.refreshToken(refreshToken)
    }
}
