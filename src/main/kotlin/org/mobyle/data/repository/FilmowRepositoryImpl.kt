package org.mobyle.data.repository

import org.mobyle.data.remote.filmow.FilmowDataSource
import org.mobyle.domain.model.FilmowProfile
import org.mobyle.domain.repository.FilmowRepository

class FilmowRepositoryImpl(
    private val filmowDataSource: FilmowDataSource
) : FilmowRepository {

    override suspend fun scrapeProfile(cookies: String, username: String): FilmowProfile {
        return filmowDataSource.scrapeProfile(cookies, username)
    }
}
