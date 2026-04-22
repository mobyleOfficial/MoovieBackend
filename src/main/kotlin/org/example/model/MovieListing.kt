package org.example.model

import kotlinx.serialization.Serializable
import org.example.domain.model.Movie

@Serializable
data class MovieListing(
    val totalPages: Int,
    val totalResults: Int,
    val movies: List<Movie>
)
