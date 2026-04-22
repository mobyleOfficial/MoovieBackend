package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.model.Country
import org.mobyle.domain.repository.MoviesRepository

class GetCountries(private val repository: MoviesRepository) {
    operator fun invoke(): List<Country> = runBlocking {
        repository.getCountries()
    }
}
