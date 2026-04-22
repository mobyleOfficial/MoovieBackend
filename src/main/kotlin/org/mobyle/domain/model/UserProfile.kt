package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val photoUrl: String? = null,
    val username: String,
    val bio: String? = null,
    val moviesWatched: List<Movie> = emptyList(),
    val following: List<UserSummary> = emptyList(),
    val followers: List<UserSummary> = emptyList()
)

@Serializable
data class UserSummary(
    val id: String,
    val username: String,
    val photoUrl: String? = null
)
