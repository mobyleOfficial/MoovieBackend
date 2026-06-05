package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.model.Country;
import com.moovie.backend.domain.repository.MoviesRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetCountries {

    private final MoviesRepository moviesRepository;

    public GetCountries(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public List<Country> invoke() {
        return moviesRepository.getCountries();
    }
}
