package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.repository.MoviesRepository;
import com.moovie.backend.domain.model.MovieListing;
import org.springframework.stereotype.Service;

@Service
public class DiscoverMovies {

    private final MoviesRepository moviesRepository;

    public DiscoverMovies(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public MovieListing invoke(int page, String year, String releaseDateGte, String releaseDateLte,
                                String sortBy, String withGenres, String withOriginalLanguage,
                                String withOriginCountry, String voteCountGte) {
        return moviesRepository.discoverMovies(page, year, releaseDateGte, releaseDateLte,
                sortBy, withGenres, withOriginalLanguage, withOriginCountry, voteCountGte);
    }
}
