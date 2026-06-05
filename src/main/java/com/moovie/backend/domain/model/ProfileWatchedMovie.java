package com.moovie.backend.domain.model;

public record ProfileWatchedMovie(
        int id,
        String title,
        String posterPath,
        double rating
) {
}
