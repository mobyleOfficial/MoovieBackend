package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.model.Genre;
import com.moovie.backend.domain.repository.MoviesRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetGenres {

    private final MoviesRepository moviesRepository;

    public GetGenres(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public List<Genre> invoke() {
        return moviesRepository.getGenres();
    }
}
