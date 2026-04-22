package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.repository.MoviesRepository
import org.example.model.MovieListing

class GetTrendingMovies(private val repository: MoviesRepository) {
    operator fun invoke(page: Int): MovieListing = runBlocking {
        repository.getTrendingMovies(page)
    }
}
