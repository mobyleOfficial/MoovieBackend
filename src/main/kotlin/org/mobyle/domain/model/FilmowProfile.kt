package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FilmowList(
    val filmowId: String,
    val title: String,
    val description: String? = null,
    val filmowUrl: String,
    val coverUrl: String? = null,
    val movies: List<Movie> = emptyList(),
    val movieCount: Int = movies.size
)

@Serializable
data class FilmowProfile(
    val username: String,
    val displayName: String,
    val watchedCount: Int = 0,
    val recentlyWatched: List<Movie> = emptyList(),
    val recentlyWatchedCount: Int = recentlyWatched.size,
    val watched: List<Movie> = emptyList(),
    val watchedMoviesCount: Int = watched.size,
    val watchlist: List<Movie> = emptyList(),
    val watchlistCount: Int = watchlist.size,
    val favorites: List<Movie> = emptyList(),
    val favoritesCount: Int = favorites.size,
    val lists: List<FilmowList> = emptyList(),
    val listsCount: Int = lists.size,
    val errors: List<String> = emptyList()
)