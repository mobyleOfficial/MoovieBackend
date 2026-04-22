package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.model.Language
import org.example.domain.repository.MoviesRepository

class GetLanguages(private val repository: MoviesRepository) {
    operator fun invoke(): List<Language> = runBlocking {
        repository.getLanguages()
    }
}
