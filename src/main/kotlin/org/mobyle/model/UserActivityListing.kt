package org.mobyle.model

import kotlinx.serialization.Serializable
import org.mobyle.domain.model.UserActivity

@Serializable
data class UserActivityListing(
    val totalPages: Int,
    val totalResults: Int,
    val activities: List<UserActivity>
)
