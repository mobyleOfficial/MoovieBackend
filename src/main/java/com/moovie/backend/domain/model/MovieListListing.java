package com.moovie.backend.domain.model;

import java.util.List;

public record MovieListListing(
        int totalPages,
        int totalResults,
        List<MovieList> lists
) {
}
