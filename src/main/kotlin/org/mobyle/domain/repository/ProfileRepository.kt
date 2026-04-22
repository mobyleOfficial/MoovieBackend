package org.mobyle.domain.repository

import org.mobyle.domain.model.PublicProfile
import org.mobyle.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getUserProfile(): UserProfile
    suspend fun updateUserProfile(profile: UserProfile)
    suspend fun getPublicProfile(userId: String): PublicProfile
}
