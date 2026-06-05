package com.moovie.backend.domain.model;

import java.util.List;

public record MovieReviewDraft(
        String id,
        int movieId,
        String movieTitle,
        String posterPath,
        String reviewTitle,
        String reviewBody,
        double rating,
        boolean isFavorite,
        boolean isRewatch,
        List<String> tags
) {
}
