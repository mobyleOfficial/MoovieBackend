package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.model.MovieListDetail
import org.example.domain.repository.MoviesRepository

class GetMovieListDetail(private val repository: MoviesRepository) {
    operator fun invoke(listId: Int, page: Int): MovieListDetail = runBlocking {
        repository.getMovieListDetail(listId, page)
    }
}
