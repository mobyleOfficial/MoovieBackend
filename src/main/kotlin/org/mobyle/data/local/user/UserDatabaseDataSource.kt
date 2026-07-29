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
import org.mobyle.domain.model.User

interface UserDatabaseDataSource {
    fun findByEmail(email: String): User?
    fun save(user: User): User
    fun findByUsername(prefix: String): List<String>
    fun countWatchedMovies(userExternalId: String): Int
    fun countFollowing(userExternalId: String): Int
    fun countFollowers(userExternalId: String): Int
    fun getRecentWatchedMovies(userExternalId: String, limit: Int): List<Movie>
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
                .map { row ->
                    Movie(
                        id = row[MoviesTable.tmdbId],
                        title = row[MoviesTable.title],
                        originalTitle = row[MoviesTable.originalTitle],
                        posterPath = row[MoviesTable.posterPath],
                        releaseDate = row[MoviesTable.year]?.toString()
                    )
                }
        }
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
