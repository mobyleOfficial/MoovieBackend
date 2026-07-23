package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val photoUrl: String = "",
    val username: String,
    val bio: String = "",
    val moviesWatchedCount: Int = 0,
    val followingCount: Int = 0,
    val followersCount: Int = 0
)
