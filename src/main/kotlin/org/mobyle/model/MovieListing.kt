package org.mobyle.model

import kotlinx.serialization.Serializable
import org.mobyle.domain.model.Movie

@Serializable
data class MovieListing(
    val totalPages: Int,
    val totalResults: Int,
    val movies: List<Movie>
)
