package com.moovie.backend.domain.model;

public record ProfileWatchlistItem(
        int id,
        String title,
        String posterPath
) {
}
