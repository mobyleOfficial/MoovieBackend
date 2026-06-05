package com.moovie.backend.domain.model;

import java.util.List;

public record MovieDetail(
        int id,
        String title,
        String overview,
        String posterPath,
        String backdropPath,
        double voteAverage,
        String releaseDate,
        String tagline,
        Integer runtime,
        List<String> genres,
        String director,
        List<String> cast,
        List<WatchProvider> watchProviders,
        List<Movie> similarMovies,
        List<MovieReview> popularReviews,
        int reviewCount,
        int listCount,
        int likeCount
) {
}
