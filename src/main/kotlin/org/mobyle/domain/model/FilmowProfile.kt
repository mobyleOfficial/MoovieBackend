package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FilmowMovie(
    val filmowId: String,
    val title: String,
    val year: String? = null,
    val filmowUrl: String,
    val posterUrl: String? = null,
    val globalRating: Double? = null,
    val userRating: Int? = null,
    val status: String,
    val director: String? = null
)

@Serializable
data class FilmowProfile(
    val username: String,
    val displayName: String,
    val watched: List<FilmowMovie> = emptyList(),
    val watchlist: List<FilmowMovie> = emptyList(),
    val favorites: List<FilmowMovie> = emptyList(),
    val watchedSeries: List<FilmowMovie> = emptyList(),
    val watchlistSeries: List<FilmowMovie> = emptyList(),
    val errors: List<String> = emptyList()
)

@Serializable
data class FilmowScrapeRequest(
    val cookies: String = "",
    val username: String
)
