package org.mobyle.data.remote.filmow

import org.mobyle.domain.model.FilmowProfile

interface FilmowDataSource {
    suspend fun scrapeProfile(cookies: String, username: String): FilmowProfile
}
