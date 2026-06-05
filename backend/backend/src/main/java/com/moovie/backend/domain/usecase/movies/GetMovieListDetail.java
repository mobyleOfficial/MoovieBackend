package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.model.MovieListDetail;
import com.moovie.backend.domain.repository.MoviesRepository;
import org.springframework.stereotype.Service;

@Service
public class GetMovieListDetail {

    private final MoviesRepository moviesRepository;

    public GetMovieListDetail(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public MovieListDetail invoke(int listId, int page) {
        return moviesRepository.getMovieListDetail(listId, page);
    }
}
