package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.model.MovieDetail
import org.mobyle.domain.repository.MoviesRepository

class LookupMovieDetail(private val repository: MoviesRepository) {
    operator fun invoke(movieId: Int?, filmowId: String?, title: String?): MovieDetail? = runBlocking {
        repository.lookupMovieDetail(movieId, filmowId, title)
    }
}
