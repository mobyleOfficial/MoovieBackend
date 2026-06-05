package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.repository.MoviesRepository;
import com.moovie.backend.domain.model.MovieListListing;
import org.springframework.stereotype.Service;

@Service
public class GetFeaturedLists {

    private final MoviesRepository moviesRepository;

    public GetFeaturedLists(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public MovieListListing invoke(int page) {
        return moviesRepository.getFeaturedLists(page);
    }
}
