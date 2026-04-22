package org.mobyle.domain.usecase.profile

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.model.UserProfile
import org.mobyle.domain.repository.ProfileRepository

class GetUserProfile(private val repository: ProfileRepository) {
    operator fun invoke(): UserProfile = runBlocking {
        repository.getUserProfile()
    }
}
