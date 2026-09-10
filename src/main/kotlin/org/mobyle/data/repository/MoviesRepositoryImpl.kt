package org.mobyle.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mobyle.data.local.movies.MovieCatalogDataSource
import org.mobyle.data.local.user.UserDatabaseDataSource
import org.mobyle.data.remote.tmdb.TmdbDataSource
import org.mobyle.data.remote.tmdb.toDomain
import org.mobyle.data.service.MovieEnrichmentService
import org.mobyle.domain.model.*
import org.mobyle.domain.repository.MoviesRepository
import org.mobyle.model.MovieListing
import org.mobyle.model.MovieListListing
import org.mobyle.model.MovieReviewListing
import org.slf4j.LoggerFactory

class MoviesRepositoryImpl(
    private val tmdbDataSource: TmdbDataSource,
    private val userDatabaseDataSource: UserDatabaseDataSource,
    private val movieCatalogDataSource: MovieCatalogDataSource,
    private val enrichmentService: MovieEnrichmentService
) : MoviesRepository {

    private val log = LoggerFactory.getLogger(MoviesRepositoryImpl::class.java)

    override suspend fun getTrendingMovies(page: Int): MovieListing {
        val listing = tmdbDataSource.getTrendingMovies(page).toDomain()
        cacheMovieListAsync(listing.movies)
        return listing
    }

    override suspend fun getMovieDetail(movieId: Int): MovieDetail {
        // If negative ID, resolve to real TMDB ID first
        if (movieId < 0) {
            val resolved = resolveNegativeId(movieId)
            if (resolved != null) return resolved
        }

        val local = movieCatalogDataSource.getLocalMovieDetail(movieId)
        if (local != null) {
            // Check if similars/providers need refresh
            val similarsCache = movieCatalogDataSource.getSimilarMovies(movieId)
            val providersCache = movieCatalogDataSource.getWatchProviders(movieId)

            if (similarsCache.isStale || providersCache.isStale) {
                refreshVolatileData(movieId)
            }

            return local.copy(
                similarMovies = similarsCache.data,
                watchProviders = providersCache.data
            )
        }

        return fetchAndCacheDetail(movieId)
    }

    override suspend fun lookupMovieDetail(movieId: Int?, filmowId: String?, title: String?): MovieDetail? {
        // 1. By TMDB ID (positive)
        if (movieId != null && movieId > 0) {
            return getMovieDetail(movieId)
        }

        // 2. By filmowId → find in local DB, then search TMDB by title
        if (filmowId != null) {
            val movie = movieCatalogDataSource.findByFilmowId(filmowId)
            println("[LOOKUP] filmowId=$filmowId → DB result: ${movie?.title} (id=${movie?.id})")
            if (movie != null) {
                val year = movie.releaseDate?.take(4)?.toIntOrNull()
                val searchResult = tmdbDataSource.searchMovies(movie.title, page = 1, year = year)
                println("[LOOKUP] TMDB search '${movie.title}' year=$year → ${searchResult.results.size} results")
                val bestMatch = searchResult.results.firstOrNull()
                if (bestMatch != null) return getMovieDetail(bestMatch.id)

                val localTitle = movie.localTitle
                if (localTitle != null) {
                    val fallbackResult = tmdbDataSource.searchMovies(localTitle, page = 1, year = year)
                    println("[LOOKUP] TMDB fallback search '${localTitle}' year=$year → ${fallbackResult.results.size} results")
                    val fallbackMatch = fallbackResult.results.firstOrNull()
                    if (fallbackMatch != null) return getMovieDetail(fallbackMatch.id)
                }
            }
        }

        // 3. By negative TMDB ID (from scraper hash)
        if (movieId != null && movieId < 0) {
            val resolved = resolveNegativeId(movieId)
            if (resolved != null) return resolved
        }

        // 4. By title → try local DB first, then TMDB
        if (title != null) {
            val localMovie = movieCatalogDataSource.findByTitle(title)
            if (localMovie != null) {
                if (localMovie.id > 0) return getMovieDetail(localMovie.id)
                val resolved = resolveNegativeId(localMovie.id)
                if (resolved != null) return resolved
            }

            val searchResult = tmdbDataSource.searchMovies(title, page = 1)
            val bestMatch = searchResult.results.firstOrNull() ?: return null
            return getMovieDetail(bestMatch.id)
        }

        return null
    }

    private suspend fun resolveNegativeId(negativeId: Int): MovieDetail? {
        val movie = movieCatalogDataSource.findByTmdbId(negativeId) ?: return null

        val year = movie.releaseDate?.take(4)?.toIntOrNull()
        val searchResult = tmdbDataSource.searchMovies(movie.title, page = 1, year = year)
        val bestMatch = searchResult.results.firstOrNull() ?: return null

        // Resolve the placeholder
        val dbId = movieCatalogDataSource.getDbIdByFilmowId(movie.filmowId ?: "")
        if (dbId != null) {
            movieCatalogDataSource.resolveScrapedMovie(
                oldDbId = dbId,
                realTmdbId = bestMatch.id,
                filmowId = movie.filmowId
            )
        }

        return fetchAndCacheDetail(bestMatch.id)
    }

    private suspend fun fetchAndCacheDetail(tmdbId: Int): MovieDetail {
        val response = tmdbDataSource.getMovieDetail(tmdbId)
        val detail = response.toDomain()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                movieCatalogDataSource.upsertMovie(
                    Movie(
                        id = tmdbId, title = detail.title, originalTitle = detail.originalTitle,
                        overview = detail.overview,
                        posterPath = detail.posterPath, backdropPath = detail.backdropPath,
                        voteAverage = detail.voteAverage, releaseDate = detail.releaseDate
                    )
                )
                movieCatalogDataSource.enrichMovie(tmdbId, detail, response.credits)
                movieCatalogDataSource.saveSimilarMovies(tmdbId, detail.similarMovies)
                movieCatalogDataSource.saveWatchProviders(tmdbId, detail.watchProviders)
            } catch (e: Exception) {
                log.warn("Failed to cache movie detail $tmdbId: ${e.message}")
            }
        }

        return detail
    }

    private fun refreshVolatileData(tmdbId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = tmdbDataSource.getMovieDetail(tmdbId)
                val detail = response.toDomain()
                movieCatalogDataSource.saveSimilarMovies(tmdbId, detail.similarMovies)
                movieCatalogDataSource.saveWatchProviders(tmdbId, detail.watchProviders)
            } catch (e: Exception) {
                log.warn("Failed to refresh volatile data for $tmdbId: ${e.message}")
            }
        }
    }

    override suspend fun searchMovies(query: String, page: Int): MovieListing {
        val listing = tmdbDataSource.searchMovies(query, page).toDomain()
        cacheMovieListAsync(listing.movies)
        return listing
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
        val listing = tmdbDataSource.discoverMovies(
            page, year, releaseDateGte, releaseDateLte, sortBy, genres, language, country, voteCountGte
        ).toDomain()
        cacheMovieListAsync(listing.movies)
        return listing
    }

    private fun cacheMovieListAsync(movies: List<Movie>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                movieCatalogDataSource.cacheMovieList(movies)
            } catch (e: Exception) {
                log.warn("Failed to cache movie list: ${e.message}")
            }
        }
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

    override suspend fun getUserWatchedMovies(userId: String, page: Int): MovieListing {
        return userDatabaseDataSource.getWatchedMovies(userId, page)
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
