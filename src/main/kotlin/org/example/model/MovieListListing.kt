package org.example.model

import kotlinx.serialization.Serializable
import org.example.domain.model.MovieList

@Serializable
data class MovieListListing(
    val totalPages: Int,
    val totalResults: Int,
    val lists: List<MovieList>
)
