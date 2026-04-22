package org.example.domain.usecase.profile

import kotlinx.coroutines.runBlocking
import org.example.domain.model.PublicProfile
import org.example.domain.repository.ProfileRepository

class GetPublicProfile(private val repository: ProfileRepository) {
    operator fun invoke(userId: String): PublicProfile = runBlocking {
        repository.getPublicProfile(userId)
    }
}
