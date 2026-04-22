package org.example.model

import kotlinx.serialization.Serializable
import org.example.domain.model.UserActivity

@Serializable
data class UserActivityListing(
    val totalPages: Int,
    val totalResults: Int,
    val activities: List<UserActivity>
)
