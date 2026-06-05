package com.moovie.backend.domain.model;

public record ProfileRecentActivity(
        String action,
        String movieTitle,
        String date
) {
}
