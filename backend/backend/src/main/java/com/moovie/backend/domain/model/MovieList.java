package com.moovie.backend.domain.model;

import java.util.List;

public record MovieList(
        int id,
        String name,
        String creator,
        String description,
        int movieCount,
        List<String> posterPaths
) {
}
