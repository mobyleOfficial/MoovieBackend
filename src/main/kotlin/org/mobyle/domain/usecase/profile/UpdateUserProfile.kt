package org.mobyle.domain.usecase.profile

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.model.UserProfile
import org.mobyle.domain.repository.ProfileRepository

class UpdateUserProfile(private val repository: ProfileRepository) {
    operator fun invoke(profile: UserProfile) = runBlocking {
        repository.updateUserProfile(profile)
    }
}
