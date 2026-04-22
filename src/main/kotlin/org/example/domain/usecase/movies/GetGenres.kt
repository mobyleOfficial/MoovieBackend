package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.model.Genre
import org.example.domain.repository.MoviesRepository

class GetGenres(private val repository: MoviesRepository) {
    operator fun invoke(): List<Genre> = runBlocking {
        repository.getGenres()
    }
}
