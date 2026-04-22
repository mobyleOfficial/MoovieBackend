package org.mobyle.model

import kotlinx.serialization.Serializable
import org.mobyle.domain.model.MovieList

@Serializable
data class MovieListListing(
    val totalPages: Int,
    val totalResults: Int,
    val lists: List<MovieList>
)
