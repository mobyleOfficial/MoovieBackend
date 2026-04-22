package org.mobyle.model

import kotlinx.serialization.Serializable
import org.mobyle.domain.model.MovieReview

@Serializable
data class MovieReviewListing(
    val totalPages: Int,
    val totalResults: Int,
    val reviews: List<MovieReview>
)
