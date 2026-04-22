package org.example.model

import kotlinx.serialization.Serializable
import org.example.domain.model.MovieReview

@Serializable
data class MovieReviewListing(
    val totalPages: Int,
    val totalResults: Int,
    val reviews: List<MovieReview>
)
