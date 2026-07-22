package org.mobyle.domain.usecase.filmow

import org.mobyle.domain.model.FilmowProfile
import org.mobyle.domain.repository.FilmowRepository

class ScrapeFilmowProfile(
    private val repository: FilmowRepository
) {
    suspend operator fun invoke(cookies: String = "", username: String): FilmowProfile {
        require(username.isNotBlank()) { "Username must not be blank" }

        return repository.scrapeProfile(cookies, username)
    }
}
