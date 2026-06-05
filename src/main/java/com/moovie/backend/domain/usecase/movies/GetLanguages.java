package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.model.Language;
import com.moovie.backend.domain.repository.MoviesRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetLanguages {

    private final MoviesRepository moviesRepository;

    public GetLanguages(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public List<Language> invoke() {
        return moviesRepository.getLanguages();
    }
}
