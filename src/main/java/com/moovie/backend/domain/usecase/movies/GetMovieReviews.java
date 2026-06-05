package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.repository.MoviesRepository;
import com.moovie.backend.domain.model.MovieReviewListing;
import org.springframework.stereotype.Service;

@Service
public class GetMovieReviews {

    private final MoviesRepository moviesRepository;

    public GetMovieReviews(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public MovieReviewListing invoke(int movieId, int page) {
        return moviesRepository.getMovieReviews(movieId, page);
    }
}
