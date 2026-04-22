package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MovieReviewDraft(
    val id: String? = null,
    val movieId: Int,
    val movieTitle: String,
    val posterPath: String? = null,
    val reviewTitle: String,
    val reviewBody: String,
    val rating: Double,
    val isFavorite: Boolean = false,
    val isRewatch: Boolean = false,
    val tags: List<String> = emptyList()
)
