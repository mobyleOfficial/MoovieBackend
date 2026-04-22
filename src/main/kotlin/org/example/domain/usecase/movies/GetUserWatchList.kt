package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.repository.MoviesRepository
import org.example.model.MovieListing

class GetUserWatchList(private val repository: MoviesRepository) {
    operator fun invoke(userId: String, page: Int): MovieListing = runBlocking {
        repository.getUserWatchList(userId, page)
    }
}
