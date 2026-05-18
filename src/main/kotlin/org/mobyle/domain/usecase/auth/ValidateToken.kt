package org.mobyle.domain.usecase.auth

import org.mobyle.domain.model.JWTClaims
import org.mobyle.domain.repository.AuthRepository

class ValidateToken(private val authRepository: AuthRepository) {
    suspend operator fun invoke(token: String): Result<JWTClaims> {
        return authRepository.validateToken(token)
    }
}
