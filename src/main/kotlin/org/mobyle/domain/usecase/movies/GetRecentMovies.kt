package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.repository.MoviesRepository
import org.mobyle.model.MovieListing

class GetRecentMovies(private val repository: MoviesRepository) {
    operator fun invoke(userId: String, limit: Int = 10): MovieListing = runBlocking {
        repository.getRecentMovies(userId, limit)
    }
}
