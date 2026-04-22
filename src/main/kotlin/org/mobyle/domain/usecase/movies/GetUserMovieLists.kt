package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.repository.MoviesRepository
import org.mobyle.model.MovieListListing

class GetUserMovieLists(private val repository: MoviesRepository) {
    operator fun invoke(page: Int): MovieListListing = runBlocking {
        repository.getUserMovieLists(page)
    }
}
