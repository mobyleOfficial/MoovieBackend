package org.mobyle.data.remote

import org.mobyle.data.remote.model.*

interface TmdbDataSource {
    suspend fun getTrendingMovies(page: Int): TmdbMovieListResponse
    suspend fun getMovieDetail(movieId: Int): TmdbMovieDetailResponse
    suspend fun searchMovies(query: String, page: Int): TmdbMovieListResponse
    suspend fun discoverMovies(
        page: Int,
        year: Int?,
        releaseDateGte: String?,
        releaseDateLte: String?,
        sortBy: String?,
        genres: String?,
        language: String?,
        country: String?,
        voteCountGte: Int?
    ): TmdbMovieListResponse
    suspend fun getGenres(): TmdbGenreListResponse
    suspend fun getCountries(): List<TmdbCountry>
    suspend fun getLanguages(): List<TmdbLanguage>
    suspend fun getMovieReviews(movieId: Int, page: Int): TmdbReviewListResponse
}
