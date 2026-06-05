package com.moovie.backend.domain.model;

public record UserActivity(
        String userName,
        String action,
        String movie,
        String time
) {
}
