package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.repository.MoviesRepository
import org.example.model.MovieListListing

class GetUserMovieLists(private val repository: MoviesRepository) {
    operator fun invoke(page: Int): MovieListListing = runBlocking {
        repository.getUserMovieLists(page)
    }
}
