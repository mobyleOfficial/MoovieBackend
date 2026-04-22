package org.example.domain.usecase.profile

import kotlinx.coroutines.runBlocking
import org.example.domain.model.UserProfile
import org.example.domain.repository.ProfileRepository

class UpdateUserProfile(private val repository: ProfileRepository) {
    operator fun invoke(profile: UserProfile) = runBlocking {
        repository.updateUserProfile(profile)
    }
}
