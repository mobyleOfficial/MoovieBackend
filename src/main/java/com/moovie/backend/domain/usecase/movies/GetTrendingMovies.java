package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.repository.MoviesRepository;
import com.moovie.backend.domain.model.MovieListing;
import org.springframework.stereotype.Service;

@Service
public class GetTrendingMovies {

    private final MoviesRepository moviesRepository;

    public GetTrendingMovies(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public MovieListing invoke(int page) {
        return moviesRepository.getTrendingMovies(page);
    }
}
