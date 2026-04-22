package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.model.Language
import org.mobyle.domain.repository.MoviesRepository

class GetLanguages(private val repository: MoviesRepository) {
    operator fun invoke(): List<Language> = runBlocking {
        repository.getLanguages()
    }
}
