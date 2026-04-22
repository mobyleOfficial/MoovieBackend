package org.example.domain.repository

import org.example.domain.model.PublicProfile
import org.example.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getUserProfile(): UserProfile
    suspend fun updateUserProfile(profile: UserProfile)
    suspend fun getPublicProfile(userId: String): PublicProfile
}
