package org.mobyle.data.repository

import org.mobyle.data.local.user.UserDatabaseDataSource
import org.mobyle.data.remote.tmdb.TmdbDataSource
import org.mobyle.data.remote.tmdb.toDomain
import org.mobyle.domain.model.*
import org.mobyle.domain.repository.MoviesRepository
import org.mobyle.model.MovieListing
import org.mobyle.model.MovieListListing
import org.mobyle.model.MovieReviewListing

class MoviesRepositoryImpl(
    private val tmdbDataSource: TmdbDataSource,
    private val userDatabaseDataSource: UserDatabaseDataSource
) : MoviesRepository {

    override suspend fun getTrendingMovies(page: Int): MovieListing {
        return tmdbDataSource.getTrendingMovies(page).toDomain()
    }

    override suspend fun getMovieDetail(movieId: Int): MovieDetail {
        return tmdbDataSource.getMovieDetail(movieId).toDomain()
    }

    override suspend fun searchMovies(query: String, page: Int): MovieListing {
        return tmdbDataSource.searchMovies(query, page).toDomain()
    }

    override suspend fun discoverMovies(
        page: Int,
        year: Int?,
        releaseDateGte: String?,
        releaseDateLte: String?,
        sortBy: String?,
        genres: String?,
        language: String?,
        country: String?,
        voteCountGte: Int?
    ): MovieListing {
        return tmdbDataSource.discoverMovies(
            page, year, releaseDateGte, releaseDateLte, sortBy, genres, language, country, voteCountGte
        ).toDomain()
    }

    override suspend fun getGenres(): List<Genre> {
        return tmdbDataSource.getGenres().genres.map { it.toDomain() }
    }

    override suspend fun getCountries(): List<Country> {
        return tmdbDataSource.getCountries().map { it.toDomain() }
    }

    override suspend fun getLanguages(): List<Language> {
        return tmdbDataSource.getLanguages().map { it.toDomain() }
    }

    override suspend fun getMovieReviews(page: Int, userId: String?, movieId: Int?): MovieReviewListing {
        if (movieId != null) {
            return tmdbDataSource.getMovieReviews(movieId, page).toDomain()
        }
        return MovieReviewListing(totalPages = 0, totalResults = 0, reviews = emptyList())
    }

    override suspend fun getUserFavoriteMovies(userId: String, page: Int): MovieListing {
        return userDatabaseDataSource.getFavoriteMovies(userId, page)
    }

    override suspend fun getUserWatchList(userId: String, page: Int): MovieListing {
        return userDatabaseDataSource.getWatchlistMovies(userId, page)
    }

    override suspend fun getMovieLists(page: Int, userId: String?): MovieListListing {
        if (userId == null) {
            return MovieListListing(totalPages = 0, totalResults = 0, lists = emptyList())
        }
        return userDatabaseDataSource.getUserLists(userId, page)
    }

    override suspend fun getUserMovieLists(page: Int): MovieListListing {
        // Requires authenticated userId — use GET /movies/lists?userId=xxx instead
        return MovieListListing(totalPages = 0, totalResults = 0, lists = emptyList())
    }

    override suspend fun getMovieListDetail(listId: Int, page: Int): MovieListDetail {
        return userDatabaseDataSource.getListDetail(listId.toLong(), page)
    }

    override suspend fun getFeaturedLists(page: Int): MovieListListing {
        // No featured lists concept yet
        return MovieListListing(totalPages = 0, totalResults = 0, lists = emptyList())
    }

    override suspend fun getRecentMovies(userId: String, limit: Int): MovieListing {
        val movies = userDatabaseDataSource.getRecentWatchedMovies(userId, limit)
        return MovieListing(
            totalPages = 1,
            totalResults = movies.size,
            movies = movies
        )
    }
}
