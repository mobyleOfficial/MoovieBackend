package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FilmowList(
    val filmowId: String,
    val title: String,
    val description: String? = null,
    val filmowUrl: String,
    val coverUrl: String? = null,
    val movies: List<Movie> = emptyList()
)

@Serializable
data class FilmowProfile(
    val username: String,
    val displayName: String,
    val recentlyWatched: List<Movie> = emptyList(),
    val watched: List<Movie> = emptyList(),
    val watchlist: List<Movie> = emptyList(),
    val favorites: List<Movie> = emptyList(),
    val lists: List<FilmowList> = emptyList(),
    val errors: List<String> = emptyList()
)