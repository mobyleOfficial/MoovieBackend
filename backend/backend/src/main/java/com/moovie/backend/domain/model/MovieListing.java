package com.moovie.backend.domain.model;

import java.util.List;

public record MovieListing(
        int totalPages,
        int totalResults,
        List<Movie> movies
) {
}
