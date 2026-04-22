package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.repository.MoviesRepository
import org.mobyle.model.MovieListListing

class GetMovieLists(private val repository: MoviesRepository) {
    operator fun invoke(page: Int, userId: String? = null): MovieListListing = runBlocking {
        repository.getMovieLists(page, userId)
    }
}
