package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.repository.MoviesRepository;
import com.moovie.backend.domain.model.MovieListing;
import org.springframework.stereotype.Service;

@Service
public class GetUserFavoriteMovies {

    private final MoviesRepository moviesRepository;

    public GetUserFavoriteMovies(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public MovieListing invoke(String userId, int page) {
        return moviesRepository.getUserFavoriteMovies(userId, page);
    }
}
