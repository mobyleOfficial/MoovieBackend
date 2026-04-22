package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.repository.MoviesRepository
import org.mobyle.model.MovieListing

class GetUserWatchList(private val repository: MoviesRepository) {
    operator fun invoke(userId: String, page: Int): MovieListing = runBlocking {
        repository.getUserWatchList(userId, page)
    }
}
