package org.mobyle.domain.usecase.auth

import org.mobyle.domain.repository.AuthRepository

class CheckNicknameAvailability(private val authRepository: AuthRepository) {
    suspend operator fun invoke(nickname: String): Result<Boolean> {
        return authRepository.checkNicknameAvailability(nickname)
    }
}
