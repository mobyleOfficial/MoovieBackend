package com.moovie.backend.domain.model;

import java.util.List;

public record PublicProfile(
        String id,
        String displayName,
        String initials,
        String bio,
        List<ProfileWatchedMovie> moviesWatched,
        List<ProfileUser> following,
        List<ProfileUser> followers,
        List<ProfileFavoriteMovie> favoriteMovies,
        List<ProfileRecentActivity> recentActivities,
        List<ProfileWatchlistItem> watchlist
) {
}
