package com.moovie.backend.domain.model;

public record UserSummary(
        String id,
        String username,
        String photoUrl
) {
}
