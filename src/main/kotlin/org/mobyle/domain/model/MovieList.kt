package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MovieList(
    val id: Int,
    val name: String,
    val creator: String,
    val description: String? = null,
    val movieCount: Int = 0,
    val posterPaths: List<String> = emptyList()
)
