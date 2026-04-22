package org.mobyle.domain.usecase.profile

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.model.PublicProfile
import org.mobyle.domain.repository.ProfileRepository

class GetPublicProfile(private val repository: ProfileRepository) {
    operator fun invoke(userId: String): PublicProfile = runBlocking {
        repository.getPublicProfile(userId)
    }
}
