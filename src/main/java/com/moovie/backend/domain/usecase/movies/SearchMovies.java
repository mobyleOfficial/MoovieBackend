package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.repository.MoviesRepository;
import com.moovie.backend.domain.model.MovieListing;
import org.springframework.stereotype.Service;

@Service
public class SearchMovies {

    private final MoviesRepository moviesRepository;

    public SearchMovies(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public MovieListing invoke(String query, int page) {
        return moviesRepository.searchMovies(query, page);
    }
}
