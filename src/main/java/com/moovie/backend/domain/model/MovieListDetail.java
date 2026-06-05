package com.moovie.backend.domain.model;

import java.util.List;

public record MovieListDetail(
        int id,
        String name,
        String creator,
        String description,
        int movieCount,
        List<String> posterPaths,
        List<Movie> movies,
        int totalMovies,
        int totalPages,
        int commentsCount,
        int likesCount,
        boolean isLiked,
        List<String> tags
) {
}
