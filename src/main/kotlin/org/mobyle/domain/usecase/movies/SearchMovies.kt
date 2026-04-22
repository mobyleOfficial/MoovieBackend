package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.repository.MoviesRepository
import org.mobyle.model.MovieListing

class SearchMovies(private val repository: MoviesRepository) {
    operator fun invoke(query: String, page: Int): MovieListing = runBlocking {
        repository.searchMovies(query, page)
    }
}
