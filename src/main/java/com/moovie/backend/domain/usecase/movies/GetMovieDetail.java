package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.model.MovieDetail;
import com.moovie.backend.domain.repository.MoviesRepository;
import org.springframework.stereotype.Service;

@Service
public class GetMovieDetail {

    private final MoviesRepository moviesRepository;

    public GetMovieDetail(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public MovieDetail invoke(int movieId) {
        return moviesRepository.getMovieDetail(movieId);
    }
}
