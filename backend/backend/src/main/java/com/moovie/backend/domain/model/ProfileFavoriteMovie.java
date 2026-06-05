package com.moovie.backend.domain.model;

public record ProfileFavoriteMovie(
        int id,
        String title,
        String posterPath
) {
}
