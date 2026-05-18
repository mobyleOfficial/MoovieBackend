package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    val avatar: String? = null,
    val createdAt: String
)
