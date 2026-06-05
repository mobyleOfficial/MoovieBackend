package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.repository.MoviesRepository;
import com.moovie.backend.domain.model.MovieListListing;
import org.springframework.stereotype.Service;

@Service
public class GetMovieLists {

    private final MoviesRepository moviesRepository;

    public GetMovieLists(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public MovieListListing invoke(int page, String userId) {
        return moviesRepository.getMovieLists(page, userId);
    }
}
