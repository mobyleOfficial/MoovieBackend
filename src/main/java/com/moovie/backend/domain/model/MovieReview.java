package com.moovie.backend.domain.model;

public record MovieReview(
        String id,
        String title,
        String date,
        double rating,
        String author,
        String content
) {
}
