package org.mobyle.domain.repository

import org.mobyle.domain.model.FilmowProfile

interface FilmowRepository {
    suspend fun scrapeProfile(cookies: String, username: String): FilmowProfile
}
