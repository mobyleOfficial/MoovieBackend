package com.moovie.backend.domain.model;

public record Movie(
        int id,
        String title,
        String overview,
        String posterPath,
        String backdropPath,
        double voteAverage,
        String releaseDate
) {
}
