package com.moovie.backend.domain.model;

import java.util.List;

public record MovieReviewListing(
        int totalPages,
        int totalResults,
        List<MovieReview> reviews
) {
}
