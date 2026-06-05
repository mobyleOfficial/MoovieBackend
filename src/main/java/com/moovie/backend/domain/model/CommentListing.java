package com.moovie.backend.domain.model;

import java.util.List;

public record CommentListing(
        String contentId,
        int totalPages,
        int totalResults,
        int page,
        List<Comment> comments
) {
}
