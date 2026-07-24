package org.mobyle.domain.usecase.auth

import org.mobyle.domain.model.AuthToken
import org.mobyle.domain.repository.AuthRepository

class SignUpUser(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, nickname: String): Result<AuthToken> {
        return authRepository.signUpUser(email, password, nickname)
    }
}
