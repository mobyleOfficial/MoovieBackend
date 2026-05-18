package org.mobyle.domain.usecase.auth

import org.mobyle.domain.model.AuthToken
import org.mobyle.domain.model.OAuthCallbackRequest
import org.mobyle.domain.repository.AuthRepository

class ProcessOAuthCallback(private val authRepository: AuthRepository) {
    suspend operator fun invoke(request: OAuthCallbackRequest): Result<AuthToken> {
        return authRepository.processOAuthCallback(request)
    }
}
