package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.repository.MoviesRepository
import org.mobyle.model.MovieReviewListing

class GetMovieReviews(private val repository: MoviesRepository) {
    operator fun invoke(page: Int, userId: String? = null, movieId: Int? = null): MovieReviewListing = runBlocking {
        repository.getMovieReviews(page, userId, movieId)
    }
}
