package org.mobyle.data.local.user

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.mobyle.data.local.database.MoviesTable
import org.mobyle.data.local.database.UserFollowsTable
import org.mobyle.data.local.database.UserListItemsTable
import org.mobyle.data.local.database.UserListsTable
import org.mobyle.data.local.database.UserMoviesTable
import org.mobyle.data.local.database.UsersTable
import org.mobyle.domain.model.FilmowList
import org.mobyle.domain.model.Movie
import org.mobyle.domain.model.MovieList
import org.mobyle.domain.model.MovieListDetail
import org.mobyle.domain.model.User
import org.mobyle.model.MovieListing
import org.mobyle.model.MovieListListing

interface UserDatabaseDataSource {
    fun findByEmail(email: String): User?
    fun save(user: User): User
    fun findByUsername(prefix: String): List<String>
    fun countWatchedMovies(userExternalId: String): Int
    fun countFollowing(userExternalId: String): Int
    fun countFollowers(userExternalId: String): Int
    fun getRecentWatchedMovies(userExternalId: String, limit: Int): List<Movie>
    fun getFavoriteMovies(userExternalId: String, page: Int, pageSize: Int = 20): MovieListing
    fun getWatchlistMovies(userExternalId: String, page: Int, pageSize: Int = 20): MovieListing
    fun getUserLists(userExternalId: String, page: Int, pageSize: Int = 20): MovieListListing
    fun getListDetail(listId: Long, page: Int, pageSize: Int = 20): MovieListDetail
    fun importMovies(userExternalId: String, movies: List<Movie>, status: String, isFavorite: Boolean = false)
    fun importLists(userExternalId: String, lists: List<FilmowList>)
}

class UserDatabaseDataSourceImpl : UserDatabaseDataSource {

    override fun findByEmail(email: String): User? {
        return transaction {
            UsersTable.selectAll()
                .where { UsersTable.email eq email }
                .firstOrNull()
                ?.let { row ->
                    User(
                        id = row[UsersTable.externalId],
                        email = row[UsersTable.email] ?: "",
                        username = row[UsersTable.username],
                        avatar = row[UsersTable.avatarUrl],
                        bio = row[UsersTable.bio],
                        createdAt = row[UsersTable.createdAt].toString(),
                        passwordHash = row[UsersTable.passwordHash]
                    )
                }
        }
    }

    override fun save(user: User): User {
        return transaction {
            UsersTable.insertAndGetId {
                it[externalId] = user.id
                it[username] = user.username
                it[email] = user.email
                it[avatarUrl] = user.avatar
                it[bio] = user.bio
                it[passwordHash] = user.passwordHash
                it[createdAt] = Clock.System.now()
            }
            user
        }
    }

    override fun findByUsername(prefix: String): List<String> {
        return transaction {
            UsersTable.selectAll()
                .where { UsersTable.username like "$prefix%" }
                .map { it[UsersTable.username] }
        }
    }

    private fun resolveUserDbId(userExternalId: String): Long? {
        return UsersTable.selectAll()
            .where { UsersTable.externalId eq userExternalId }
            .firstOrNull()
            ?.get(UsersTable.id)?.value
    }

    override fun countWatchedMovies(userExternalId: String): Int {
        return transaction {
            val userDbId = resolveUserDbId(userExternalId) ?: return@transaction 0

            UserMoviesTable.selectAll()
                .where {
                    (UserMoviesTable.userId eq userDbId) and
                        (UserMoviesTable.status eq "watched")
                }
                .count()
                .toInt()
        }
    }

    override fun countFollowing(userExternalId: String): Int {
        return transaction {
            val userDbId = resolveUserDbId(userExternalId) ?: return@transaction 0

            UserFollowsTable.selectAll()
                .where { UserFollowsTable.followerId eq userDbId }
                .count()
                .toInt()
        }
    }

    override fun countFollowers(userExternalId: String): Int {
        return transaction {
            val userDbId = resolveUserDbId(userExternalId) ?: return@transaction 0

            UserFollowsTable.selectAll()
                .where { UserFollowsTable.followedId eq userDbId }
                .count()
                .toInt()
        }
    }

    override fun getRecentWatchedMovies(userExternalId: String, limit: Int): List<Movie> {
        return transaction {
            val userDbId = resolveUserDbId(userExternalId) ?: return@transaction emptyList()

            (UserMoviesTable innerJoin MoviesTable)
                .selectAll()
                .where {
                    (UserMoviesTable.userId eq userDbId) and
                        (UserMoviesTable.status eq "watched")
                }
                .orderBy(UserMoviesTable.watchedAt, SortOrder.DESC)
                .limit(limit)
                .map { row -> rowToMovie(row) }
        }
    }

    override fun getFavoriteMovies(userExternalId: String, page: Int, pageSize: Int): MovieListing {
        return transaction {
            val userDbId = resolveUserDbId(userExternalId)
                ?: return@transaction MovieListing(0, 0, emptyList())

            val totalResults = UserMoviesTable.selectAll()
                .where {
                    (UserMoviesTable.userId eq userDbId) and
                        (UserMoviesTable.isFavorite eq true)
                }
                .count().toInt()

            val movies = (UserMoviesTable innerJoin MoviesTable)
                .selectAll()
                .where {
                    (UserMoviesTable.userId eq userDbId) and
                        (UserMoviesTable.isFavorite eq true)
                }
                .orderBy(UserMoviesTable.updatedAt, SortOrder.DESC)
                .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
                .map { row -> rowToMovie(row) }

            MovieListing(
                totalPages = (totalResults + pageSize - 1) / pageSize,
                totalResults = totalResults,
                movies = movies
            )
        }
    }

    override fun getWatchlistMovies(userExternalId: String, page: Int, pageSize: Int): MovieListing {
        return transaction {
            val userDbId = resolveUserDbId(userExternalId)
                ?: return@transaction MovieListing(0, 0, emptyList())

            val totalResults = UserMoviesTable.selectAll()
                .where {
                    (UserMoviesTable.userId eq userDbId) and
                        (UserMoviesTable.status eq "want_to_watch")
                }
                .count().toInt()

            val movies = (UserMoviesTable innerJoin MoviesTable)
                .selectAll()
                .where {
                    (UserMoviesTable.userId eq userDbId) and
                        (UserMoviesTable.status eq "want_to_watch")
                }
                .orderBy(UserMoviesTable.updatedAt, SortOrder.DESC)
                .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
                .map { row -> rowToMovie(row) }

            MovieListing(
                totalPages = (totalResults + pageSize - 1) / pageSize,
                totalResults = totalResults,
                movies = movies
            )
        }
    }

    override fun getUserLists(userExternalId: String, page: Int, pageSize: Int): MovieListListing {
        return transaction {
            val userDbId = resolveUserDbId(userExternalId)
                ?: return@transaction MovieListListing(0, 0, emptyList())

            val username = UsersTable.selectAll()
                .where { UsersTable.externalId eq userExternalId }
                .firstOrNull()?.get(UsersTable.username) ?: ""

            val totalResults = UserListsTable.selectAll()
                .where { UserListsTable.userId eq userDbId }
                .count().toInt()

            val lists = UserListsTable.selectAll()
                .where { UserListsTable.userId eq userDbId }
                .orderBy(UserListsTable.createdAt, SortOrder.DESC)
                .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
                .map { row ->
                    val listDbId = row[UserListsTable.id].value

                    val movieCount = UserListItemsTable.selectAll()
                        .where { UserListItemsTable.listId eq listDbId }
                        .count().toInt()

                    val posterPaths = (UserListItemsTable innerJoin MoviesTable)
                        .selectAll()
                        .where { UserListItemsTable.listId eq listDbId }
                        .orderBy(UserListItemsTable.position, SortOrder.ASC)
                        .limit(4)
                        .mapNotNull { it[MoviesTable.posterPath] }

                    MovieList(
                        id = listDbId.toInt(),
                        name = row[UserListsTable.name],
                        creator = username,
                        description = row[UserListsTable.description],
                        movieCount = movieCount,
                        posterPaths = posterPaths
                    )
                }

            MovieListListing(
                totalPages = (totalResults + pageSize - 1) / pageSize,
                totalResults = totalResults,
                lists = lists
            )
        }
    }

    override fun getListDetail(listId: Long, page: Int, pageSize: Int): MovieListDetail {
        return transaction {
            val listRow = UserListsTable.selectAll()
                .where { UserListsTable.id eq listId }
                .firstOrNull()
                ?: return@transaction MovieListDetail(
                    id = listId.toInt(), name = "", creator = ""
                )

            val userDbId = listRow[UserListsTable.userId].value
            val username = UsersTable.selectAll()
                .where { UsersTable.id eq userDbId }
                .firstOrNull()?.get(UsersTable.username) ?: ""

            val totalMovies = UserListItemsTable.selectAll()
                .where { UserListItemsTable.listId eq listId }
                .count().toInt()

            val movies = (UserListItemsTable innerJoin MoviesTable)
                .selectAll()
                .where { UserListItemsTable.listId eq listId }
                .orderBy(UserListItemsTable.position, SortOrder.ASC)
                .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
                .map { row ->
                    Movie(
                        id = row[MoviesTable.tmdbId],
                        title = row[MoviesTable.title],
                        originalTitle = row[MoviesTable.originalTitle],
                        posterPath = row[MoviesTable.posterPath],
                        releaseDate = row[MoviesTable.year]?.toString()
                    )
                }

            MovieListDetail(
                id = listId.toInt(),
                name = listRow[UserListsTable.name],
                creator = username,
                description = listRow[UserListsTable.description],
                movies = movies,
                totalMovies = totalMovies,
                totalPages = (totalMovies + pageSize - 1) / pageSize
            )
        }
    }

    private fun rowToMovie(row: org.jetbrains.exposed.sql.ResultRow): Movie {
        return Movie(
            id = row[MoviesTable.tmdbId],
            title = row[MoviesTable.title],
            originalTitle = row[MoviesTable.originalTitle],
            posterPath = row[MoviesTable.posterPath],
            voteAverage = 0.0,
            userRating = row[UserMoviesTable.rating]?.toDouble(),
            releaseDate = row[MoviesTable.year]?.toString()
        )
    }

    private fun ensureMovie(movie: Movie): Long {
        val existing = MoviesTable.selectAll()
            .where { MoviesTable.tmdbId eq movie.id }
            .firstOrNull()

        if (existing != null) return existing[MoviesTable.id].value

        return MoviesTable.insertAndGetId {
            it[tmdbId] = movie.id
            it[title] = movie.title
            it[originalTitle] = movie.originalTitle
            it[year] = movie.releaseDate?.take(4)?.toIntOrNull()
            it[posterPath] = movie.posterPath
        }.value
    }

    override fun importMovies(
        userExternalId: String,
        movies: List<Movie>,
        status: String,
        isFavorite: Boolean
    ) {
        transaction {
            val userDbId = resolveUserDbId(userExternalId) ?: return@transaction
            val now = Clock.System.now()

            for (movie in movies) {
                if (movie.id <= 0) continue
                val movieDbId = ensureMovie(movie)

                UserMoviesTable.upsert(
                    UserMoviesTable.userId, UserMoviesTable.movieId, UserMoviesTable.importSource
                ) {
                    it[userId] = userDbId
                    it[movieId] = movieDbId
                    it[UserMoviesTable.status] = status
                    it[rating] = movie.userRating?.toFloat()
                    it[UserMoviesTable.isFavorite] = isFavorite
                    it[importSource] = "filmow"
                    it[importedAt] = now
                    it[createdAt] = now
                    it[updatedAt] = now
                    if (status == "watched") {
                        it[watchedAt] = now
                    }
                }
            }
        }
    }

    override fun importLists(userExternalId: String, lists: List<FilmowList>) {
        transaction {
            val userDbId = resolveUserDbId(userExternalId) ?: return@transaction
            val now = Clock.System.now()

            for (filmowList in lists) {
                val listId = UserListsTable.insertAndGetId {
                    it[userId] = userDbId
                    it[name] = filmowList.title
                    it[description] = filmowList.description
                    it[isPublic] = true
                    it[createdAt] = now
                }

                filmowList.movies.forEachIndexed { index, movie ->
                    if (movie.id <= 0) return@forEachIndexed
                    val movieDbId = ensureMovie(movie)

                    UserListItemsTable.upsert(UserListItemsTable.listId, UserListItemsTable.movieId) {
                        it[UserListItemsTable.listId] = listId
                        it[UserListItemsTable.movieId] = movieDbId
                        it[position] = index
                        it[addedAt] = now
                    }
                }
            }
        }
    }
}
