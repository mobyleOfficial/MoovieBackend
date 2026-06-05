package com.moovie.backend.domain.model;

import java.util.List;

public record UserProfile(
        String photoUrl,
        String username,
        String bio,
        List<Movie> moviesWatched,
        List<UserSummary> following,
        List<UserSummary> followers
) {
}
