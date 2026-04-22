package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.model.MovieListDetail
import org.mobyle.domain.repository.MoviesRepository

class GetMovieListDetail(private val repository: MoviesRepository) {
    operator fun invoke(listId: Int, page: Int): MovieListDetail = runBlocking {
        repository.getMovieListDetail(listId, page)
    }
}
