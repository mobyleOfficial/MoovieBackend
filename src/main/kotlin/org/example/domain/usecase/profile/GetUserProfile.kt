package org.example.domain.usecase.profile

import kotlinx.coroutines.runBlocking
import org.example.domain.model.UserProfile
import org.example.domain.repository.ProfileRepository

class GetUserProfile(private val repository: ProfileRepository) {
    operator fun invoke(): UserProfile = runBlocking {
        repository.getUserProfile()
    }
}
