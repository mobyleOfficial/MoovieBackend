package org.mobyle.data.repository

import org.mobyle.domain.model.*
import org.mobyle.domain.repository.ProfileRepository

class ProfileRepositoryImpl : ProfileRepository {

    override suspend fun getUserProfile(): UserProfile {
        // TODO: Replace with actual user data storage
        return UserProfile(
            username = "",
            bio = null,
            photoUrl = null
        )
    }

    override suspend fun updateUserProfile(profile: UserProfile) {
        // TODO: Replace with actual user data storage
    }

    override suspend fun getPublicProfile(userId: String): PublicProfile {
        // TODO: Replace with actual user data storage
        return PublicProfile(
            id = userId,
            displayName = "",
            initials = ""
        )
    }
}
