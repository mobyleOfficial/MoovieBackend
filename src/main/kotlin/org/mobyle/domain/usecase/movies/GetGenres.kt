package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.model.Genre
import org.mobyle.domain.repository.MoviesRepository

class GetGenres(private val repository: MoviesRepository) {
    operator fun invoke(): List<Genre> = runBlocking {
        repository.getGenres()
    }
}
