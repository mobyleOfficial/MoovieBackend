package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MovieListDetail(
    val id: Int,
    val name: String,
    val creator: String,
    val description: String? = null,
    val movies: List<Movie> = emptyList(),
    val totalMovies: Int = 0,
    val totalPages: Int = 0,
    val commentsCount: Int = 0,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val tags: List<String> = emptyList()
)
