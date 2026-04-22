package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.repository.MoviesRepository
import org.example.model.MovieReviewListing

class GetMovieReviews(private val repository: MoviesRepository) {
    operator fun invoke(page: Int, userId: String? = null, movieId: Int? = null): MovieReviewListing = runBlocking {
        repository.getMovieReviews(page, userId, movieId)
    }
}
