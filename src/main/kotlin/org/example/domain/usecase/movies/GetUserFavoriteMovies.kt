package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.repository.MoviesRepository
import org.example.model.MovieListing

class GetUserFavoriteMovies(private val repository: MoviesRepository) {
    operator fun invoke(userId: String, page: Int): MovieListing = runBlocking {
        repository.getUserFavoriteMovies(userId, page)
    }
}
