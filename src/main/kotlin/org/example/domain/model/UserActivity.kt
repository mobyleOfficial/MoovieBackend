package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserActivity(
    val userName: String,
    val action: String,
    val movie: String,
    val time: String
)
