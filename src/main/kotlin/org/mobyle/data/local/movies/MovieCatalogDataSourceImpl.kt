package org.mobyle.data.local.movies

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mobyle.data.local.database.*
import org.mobyle.data.remote.tmdb.model.TmdbCredits
import org.mobyle.domain.model.Movie
import org.mobyle.domain.model.MovieDetail
import org.mobyle.domain.model.WatchProvider

class MovieCatalogDataSourceImpl : MovieCatalogDataSource {

    override fun findByTmdbId(tmdbId: Int): Movie? {
        return transaction {
            MoviesTable.selectAll()
                .where { MoviesTable.tmdbId eq tmdbId }
                .firstOrNull()
                ?.let { rowToMovie(it) }
        }
    }

    override fun findByFilmowId(filmowId: String): Movie? {
        return transaction {
            MoviesTable.selectAll()
                .where { MoviesTable.filmowId eq filmowId }
                .firstOrNull()
                ?.let { rowToMovie(it) }
        }
    }

    override fun findByLetterboxdId(letterboxdId: String): Movie? {
        return transaction {
            MoviesTable.selectAll()
                .where { MoviesTable.letterboxdId eq letterboxdId }
                .firstOrNull()
                ?.let { rowToMovie(it) }
        }
    }

    override fun getDbIdByFilmowId(filmowId: String): Long? {
        return transaction {
            MoviesTable.selectAll()
                .where { MoviesTable.filmowId eq filmowId }
                .firstOrNull()
                ?.get(MoviesTable.id)?.value
        }
    }

    override fun upsertMovie(movie: Movie, filmowId: String?, letterboxdId: String?): Long {
        return transaction {
            // 1. Check by external source ID first (handles resolved movies whose tmdbId changed)
            val bySourceId = when {
                filmowId != null -> MoviesTable.selectAll()
                    .where { MoviesTable.filmowId eq filmowId }
                    .firstOrNull()
                letterboxdId != null -> MoviesTable.selectAll()
                    .where { MoviesTable.letterboxdId eq letterboxdId }
                    .firstOrNull()
                else -> null
            }

            // 2. Fall back to tmdbId lookup
            val existing = bySourceId ?: MoviesTable.selectAll()
                .where { MoviesTable.tmdbId eq movie.id }
                .firstOrNull()

            if (existing != null) {
                val dbId = existing[MoviesTable.id].value
                MoviesTable.update({ MoviesTable.id eq dbId }) {
                    if (movie.overview.isNotBlank()) it[overview] = movie.overview
                    if (movie.posterPath != null) it[posterPath] = movie.posterPath
                    if (movie.backdropPath != null) it[backdropPath] = movie.backdropPath
                    if (movie.voteAverage > 0.0) it[voteAverage] = movie.voteAverage.toFloat()
                    if (movie.releaseDate != null) it[releaseDate] = movie.releaseDate
                    if (movie.localTitle != null) it[localTitle] = movie.localTitle
                    if (movie.originalTitle != null) it[originalTitle] = movie.originalTitle
                    if (filmowId != null) it[MoviesTable.filmowId] = filmowId
                    if (letterboxdId != null) it[MoviesTable.letterboxdId] = letterboxdId
                }
                dbId
            } else {
                MoviesTable.insertAndGetId {
                    it[tmdbId] = movie.id
                    it[title] = movie.title
                    it[localTitle] = movie.localTitle
                    it[originalTitle] = movie.originalTitle
                    it[year] = movie.releaseDate?.take(4)?.toIntOrNull()
                    it[posterPath] = movie.posterPath
                    it[backdropPath] = movie.backdropPath
                    it[overview] = movie.overview.takeIf { o -> o.isNotBlank() }
                    it[voteAverage] = movie.voteAverage.takeIf { v -> v > 0.0 }?.toFloat()
                    it[releaseDate] = movie.releaseDate
                    it[MoviesTable.filmowId] = filmowId
                    it[MoviesTable.letterboxdId] = letterboxdId
                    it[needsEnrichment] = true
                }.value
            }
        }
    }

    override fun enrichMovie(tmdbId: Int, detail: MovieDetail, credits: TmdbCredits?) {
        transaction {
            val movieRow = MoviesTable.selectAll()
                .where { MoviesTable.tmdbId eq tmdbId }
                .firstOrNull() ?: return@transaction

            val movieDbId = movieRow[MoviesTable.id].value

            MoviesTable.update({ MoviesTable.tmdbId eq tmdbId }) {
                it[overview] = detail.overview
                it[backdropPath] = detail.backdropPath
                it[releaseDate] = detail.releaseDate
                it[runtime] = detail.runtime
                it[tagline] = detail.tagline
                it[voteAverage] = detail.voteAverage.toFloat()
                it[posterPath] = detail.posterPath ?: movieRow[MoviesTable.posterPath]
                it[year] = detail.releaseDate?.take(4)?.toIntOrNull()
                it[enrichedAt] = Clock.System.now()
                it[needsEnrichment] = false
            }

            syncGenres(movieDbId, detail.genres)

            if (credits != null) {
                syncCredits(movieDbId, credits)
            }
        }
    }

    override fun resolveScrapedMovie(oldDbId: Long, realTmdbId: Int, filmowId: String?) {
        transaction {
            val existingReal = MoviesTable.selectAll()
                .where { MoviesTable.tmdbId eq realTmdbId }
                .firstOrNull()

            if (existingReal != null) {
                val realDbId = existingReal[MoviesTable.id].value
                if (realDbId == oldDbId) return@transaction

                // Copy source-specific data from placeholder to real row
                val oldRow = MoviesTable.selectAll()
                    .where { MoviesTable.id eq oldDbId }
                    .firstOrNull()

                MoviesTable.update({ MoviesTable.id eq realDbId }) {
                    if (filmowId != null) it[MoviesTable.filmowId] = filmowId
                    val oldLocalTitle = oldRow?.get(MoviesTable.localTitle)
                    if (oldLocalTitle != null && existingReal[MoviesTable.localTitle] == null) {
                        it[localTitle] = oldLocalTitle
                    }
                }

                // Re-point UserMovies references, handling duplicates
                val existingUserMovies = UserMoviesTable.selectAll()
                    .where { UserMoviesTable.movieId eq realDbId }
                    .map { Triple(it[UserMoviesTable.userId].value, it[UserMoviesTable.importSource], it[UserMoviesTable.id].value) }
                    .toSet()

                val oldUserMovies = UserMoviesTable.selectAll()
                    .where { UserMoviesTable.movieId eq oldDbId }
                    .toList()

                for (row in oldUserMovies) {
                    val userId = row[UserMoviesTable.userId].value
                    val source = row[UserMoviesTable.importSource]
                    val hasDuplicate = existingUserMovies.any { it.first == userId && it.second == source }
                    if (hasDuplicate) {
                        UserMoviesTable.deleteWhere { UserMoviesTable.id eq row[UserMoviesTable.id] }
                    } else {
                        UserMoviesTable.update({ UserMoviesTable.id eq row[UserMoviesTable.id] }) {
                            it[movieId] = realDbId
                        }
                    }
                }

                // Re-point list items, handling duplicates
                val existingListItems = UserListItemsTable.selectAll()
                    .where { UserListItemsTable.movieId eq realDbId }
                    .map { it[UserListItemsTable.listId].value }
                    .toSet()

                val oldListItems = UserListItemsTable.selectAll()
                    .where { UserListItemsTable.movieId eq oldDbId }
                    .toList()

                for (row in oldListItems) {
                    val listId = row[UserListItemsTable.listId].value
                    if (existingListItems.contains(listId)) {
                        UserListItemsTable.deleteWhere { UserListItemsTable.id eq row[UserListItemsTable.id] }
                    } else {
                        UserListItemsTable.update({ UserListItemsTable.id eq row[UserListItemsTable.id] }) {
                            it[movieId] = realDbId
                        }
                    }
                }

                // Delete orphaned placeholder
                MovieCastTable.deleteWhere { MovieCastTable.movieId eq oldDbId }
                MovieGenresTable.deleteWhere { MovieGenresTable.movieId eq oldDbId }
                MoviesTable.deleteWhere { MoviesTable.id eq oldDbId }
            } else {
                MoviesTable.update({ MoviesTable.id eq oldDbId }) {
                    it[tmdbId] = realTmdbId
                    if (filmowId != null) it[MoviesTable.filmowId] = filmowId
                }
            }
        }
    }

    override fun getMoviesNeedingEnrichment(limit: Int): List<EnrichmentCandidate> {
        return transaction {
            MoviesTable.selectAll()
                .where { MoviesTable.needsEnrichment eq true }
                .orderBy(MoviesTable.id, SortOrder.ASC)
                .limit(limit)
                .map { row ->
                    val tmdbId = row[MoviesTable.tmdbId]
                    EnrichmentCandidate(
                        dbId = row[MoviesTable.id].value,
                        tmdbId = tmdbId,
                        title = row[MoviesTable.title],
                        year = row[MoviesTable.year],
                        filmowId = row[MoviesTable.filmowId],
                        needsResolution = tmdbId < 0
                    )
                }
        }
    }

    override fun getLocalMovieDetail(tmdbId: Int): MovieDetail? {
        return transaction {
            val row = MoviesTable.selectAll()
                .where { MoviesTable.tmdbId eq tmdbId }
                .firstOrNull() ?: return@transaction null

            val enrichedAt = row[MoviesTable.enrichedAt] ?: return@transaction null
            val staleThreshold = Clock.System.now().minus(kotlin.time.Duration.parse("7d"))
            if (enrichedAt < staleThreshold) {
                // Mark for re-enrichment but still return local data
                MoviesTable.update({ MoviesTable.id eq row[MoviesTable.id] }) {
                    it[needsEnrichment] = true
                }
            }

            val movieDbId = row[MoviesTable.id].value

            val genres = (MovieGenresTable innerJoin GenresTable)
                .selectAll()
                .where { MovieGenresTable.movieId eq movieDbId }
                .map { it[GenresTable.name] }

            val director = (MovieCastTable innerJoin PeopleTable)
                .selectAll()
                .where {
                    (MovieCastTable.movieId eq movieDbId) and
                        (MovieCastTable.role eq "director")
                }
                .firstOrNull()
                ?.get(PeopleTable.name)

            val cast = (MovieCastTable innerJoin PeopleTable)
                .selectAll()
                .where {
                    (MovieCastTable.movieId eq movieDbId) and
                        (MovieCastTable.role eq "actor")
                }
                .orderBy(MovieCastTable.position, SortOrder.ASC)
                .limit(10)
                .map { it[PeopleTable.name] }

            MovieDetail(
                id = row[MoviesTable.tmdbId],
                title = row[MoviesTable.title],
                overview = row[MoviesTable.overview] ?: "",
                posterPath = row[MoviesTable.posterPath],
                backdropPath = row[MoviesTable.backdropPath],
                voteAverage = row[MoviesTable.voteAverage]?.toDouble() ?: 0.0,
                releaseDate = row[MoviesTable.releaseDate],
                tagline = row[MoviesTable.tagline],
                runtime = row[MoviesTable.runtime],
                genres = genres,
                director = director,
                cast = cast,
                watchProviders = emptyList(),
                similarMovies = emptyList(),
                popularReviews = emptyList(),
                reviewCount = 0,
                listCount = 0,
                likeCount = 0
            )
        }
    }

    override fun cacheMovieList(movies: List<Movie>) {
        transaction {
            for (movie in movies) {
                if (movie.id <= 0) continue
                val existing = MoviesTable.selectAll()
                    .where { MoviesTable.tmdbId eq movie.id }
                    .firstOrNull()

                if (existing != null) {
                    val dbId = existing[MoviesTable.id].value
                    MoviesTable.update({ MoviesTable.id eq dbId }) {
                        if (movie.posterPath != null) it[posterPath] = movie.posterPath
                        if (movie.backdropPath != null) it[backdropPath] = movie.backdropPath
                        if (movie.voteAverage > 0.0) it[voteAverage] = movie.voteAverage.toFloat()
                        if (movie.releaseDate != null) it[releaseDate] = movie.releaseDate
                        if (movie.overview.isNotBlank()) it[overview] = movie.overview
                    }
                } else {
                    val hasBasicData = movie.overview.isNotBlank()
                    MoviesTable.insertAndGetId {
                        it[tmdbId] = movie.id
                        it[title] = movie.title
                        it[originalTitle] = movie.originalTitle
                        it[year] = movie.releaseDate?.take(4)?.toIntOrNull()
                        it[posterPath] = movie.posterPath
                        it[backdropPath] = movie.backdropPath
                        it[overview] = movie.overview.takeIf { o -> o.isNotBlank() }
                        it[voteAverage] = movie.voteAverage.takeIf { v -> v > 0.0 }?.toFloat()
                        it[releaseDate] = movie.releaseDate
                        it[needsEnrichment] = !hasBasicData
                    }
                }
            }
        }
    }

    private fun syncGenres(movieDbId: Long, genreNames: List<String>) {
        for (name in genreNames) {
            val genreDbId = GenresTable.selectAll()
                .where { GenresTable.name eq name }
                .firstOrNull()
                ?.get(GenresTable.id)?.value ?: continue

            MovieGenresTable.upsert(MovieGenresTable.movieId, MovieGenresTable.genreId) {
                it[movieId] = movieDbId
                it[genreId] = genreDbId
            }
        }
    }

    private fun syncCredits(movieDbId: Long, credits: TmdbCredits) {
        // Directors
        val directors = credits.crew.filter { it.job == "Director" }
        for ((idx, crew) in directors.withIndex()) {
            val personTmdbId = crew.id ?: continue
            val personDbId = ensurePerson(personTmdbId, crew.name ?: "", crew.profilePath)
            upsertCastEntry(movieDbId, personDbId, "director", null, idx)
        }

        // Writers
        val writers = credits.crew.filter { it.job == "Writer" || it.job == "Screenplay" }
        for ((idx, crew) in writers.distinctBy { it.id }.withIndex()) {
            val personTmdbId = crew.id ?: continue
            val personDbId = ensurePerson(personTmdbId, crew.name ?: "", crew.profilePath)
            upsertCastEntry(movieDbId, personDbId, "writer", null, idx)
        }

        // Top 20 actors
        val actors = credits.cast.take(20)
        for (cast in actors) {
            val personTmdbId = cast.id ?: continue
            val personDbId = ensurePerson(personTmdbId, cast.name ?: "", cast.profilePath)
            upsertCastEntry(movieDbId, personDbId, "actor", cast.character, cast.order ?: 0)
        }
    }

    private fun upsertCastEntry(movieDbId: Long, personDbId: Long, role: String, character: String?, position: Int) {
        MovieCastTable.upsert(MovieCastTable.movieId, MovieCastTable.personId, MovieCastTable.role) {
            it[movieId] = movieDbId
            it[personId] = personDbId
            it[MovieCastTable.role] = role
            it[MovieCastTable.character] = character
            it[MovieCastTable.position] = position
        }
    }

    private fun ensurePerson(tmdbId: Int, name: String, profilePath: String?): Long {
        val existing = PeopleTable.selectAll()
            .where { PeopleTable.tmdbId eq tmdbId }
            .firstOrNull()

        if (existing != null) {
            val dbId = existing[PeopleTable.id].value
            // Update profile path if we have a new one
            if (profilePath != null && existing[PeopleTable.profilePath] == null) {
                PeopleTable.update({ PeopleTable.id eq dbId }) {
                    it[PeopleTable.profilePath] = profilePath
                }
            }
            return dbId
        }

        return PeopleTable.insertAndGetId {
            it[PeopleTable.tmdbId] = tmdbId
            it[PeopleTable.name] = name
            it[PeopleTable.profilePath] = profilePath
        }.value
    }

    override fun findByTitle(title: String): Movie? {
        return transaction {
            MoviesTable.selectAll()
                .where {
                    (MoviesTable.localTitle eq title) or
                        (MoviesTable.title eq title) or
                        (MoviesTable.originalTitle eq title)
                }
                .firstOrNull()
                ?.let { rowToMovie(it) }
        }
    }

    private fun rowToMovie(row: ResultRow): Movie {
        return Movie(
            id = row[MoviesTable.tmdbId],
            title = row[MoviesTable.title],
            localTitle = row[MoviesTable.localTitle],
            originalTitle = row[MoviesTable.originalTitle],
            overview = row[MoviesTable.overview] ?: "",
            posterPath = row[MoviesTable.posterPath],
            backdropPath = row[MoviesTable.backdropPath],
            voteAverage = row[MoviesTable.voteAverage]?.toDouble() ?: 0.0,
            releaseDate = row[MoviesTable.releaseDate] ?: row[MoviesTable.year]?.toString(),
            filmowId = row[MoviesTable.filmowId]
        )
    }
}
