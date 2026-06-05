package com.moovie.backend.domain.model;

public record Comment(
        String id,
        String authorName,
        String authorAvatar,
        String content,
        String createdAt,
        double rating
) {
}
