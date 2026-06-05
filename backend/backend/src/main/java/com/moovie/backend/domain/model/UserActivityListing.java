package com.moovie.backend.domain.model;

import java.util.List;

public record UserActivityListing(
        int totalPages,
        int totalResults,
        List<UserActivity> activities
) {
}
