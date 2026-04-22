package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.model.MovieDetail
import org.example.domain.repository.MoviesRepository

class GetMovieDetail(private val repository: MoviesRepository) {
    operator fun invoke(movieId: Int): MovieDetail = runBlocking {
        repository.getMovieDetail(movieId)
    }
}
