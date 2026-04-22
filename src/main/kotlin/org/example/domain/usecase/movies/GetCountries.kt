package org.example.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.example.domain.model.Country
import org.example.domain.repository.MoviesRepository

class GetCountries(private val repository: MoviesRepository) {
    operator fun invoke(): List<Country> = runBlocking {
        repository.getCountries()
    }
}
