package com.moovie.backend.domain.usecase.movies;

import com.moovie.backend.domain.repository.MoviesRepository;
import com.moovie.backend.domain.model.MovieListing;
import org.springframework.stereotype.Service;

@Service
public class GetUserWatchList {

    private final MoviesRepository moviesRepository;

    public GetUserWatchList(MoviesRepository moviesRepository) {
        this.moviesRepository = moviesRepository;
    }

    public MovieListing invoke(String userId, int page) {
        return moviesRepository.getUserWatchList(userId, page);
    }
}
