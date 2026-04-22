package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.repository.MoviesRepository
import org.example.model.MovieListListing

class GetMovieLists(private val repository: MoviesRepository) {
    operator fun invoke(page: Int, userId: String? = null): MovieListListing = runBlocking {
        repository.getMovieLists(page, userId)
    }
}
