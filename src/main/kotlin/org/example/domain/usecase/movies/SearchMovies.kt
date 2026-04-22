package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.repository.MoviesRepository
import org.example.model.MovieListing

class SearchMovies(private val repository: MoviesRepository) {
    operator fun invoke(query: String, page: Int): MovieListing = runBlocking {
        repository.searchMovies(query, page)
    }
}
