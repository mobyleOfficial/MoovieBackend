package org.example.domain.repository

import org.example.domain.model.*
import org.example.model.MovieListing
import org.example.model.MovieListListing
import org.example.model.MovieReviewListing

interface MoviesRepository {
    suspend fun getTrendingMovies(page: Int): MovieListing
    suspend fun getMovieDetail(movieId: Int): MovieDetail
    suspend fun searchMovies(query: String, page: Int): MovieListing
    suspend fun discoverMovies(
        page: Int,
        year: Int? = null,
        releaseDateGte: String? = null,
        releaseDateLte: String? = null,
        sortBy: String? = null,
        genres: String? = null,
        language: String? = null,
        country: String? = null,
        voteCountGte: Int? = null
    ): MovieListing
    suspend fun getGenres(): List<Genre>
    suspend fun getCountries(): List<Country>
    suspend fun getLanguages(): List<Language>
    suspend fun getMovieReviews(page: Int, userId: String?, movieId: Int?): MovieReviewListing
    suspend fun getUserFavoriteMovies(userId: String, page: Int): MovieListing
    suspend fun getUserWatchList(userId: String, page: Int): MovieListing
    suspend fun getMovieLists(page: Int, userId: String?): MovieListListing
    suspend fun getUserMovieLists(page: Int): MovieListListing
    suspend fun getMovieListDetail(listId: Int, page: Int): MovieListDetail
    suspend fun getFeaturedLists(page: Int): MovieListListing
}
