package com.moovie.backend.domain.model;

public record ProfileUser(
        String id,
        String displayName,
        String initials
) {
}
