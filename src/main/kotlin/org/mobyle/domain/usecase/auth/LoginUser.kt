package org.mobyle.domain.usecase.auth

import org.mobyle.domain.model.AuthToken
import org.mobyle.domain.repository.AuthRepository

class LoginUser(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthToken> {
        return authRepository.loginUser(email, password)
    }
}
