package org.mobyle.domain.usecase.movies

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.repository.MoviesRepository
import org.mobyle.model.MovieListing

class DiscoverMovies(private val repository: MoviesRepository) {
    operator fun invoke(
        page: Int,
        year: Int? = null,
        releaseDateGte: String? = null,
        releaseDateLte: String? = null,
        sortBy: String? = null,
        genres: String? = null,
        language: String? = null,
        country: String? = null,
        voteCountGte: Int? = null
    ): MovieListing = runBlocking {
        repository.discoverMovies(
            page = page,
            year = year,
            releaseDateGte = releaseDateGte,
            releaseDateLte = releaseDateLte,
            sortBy = sortBy,
            genres = genres,
            language = language,
            country = country,
            voteCountGte = voteCountGte
        )
    }
}
