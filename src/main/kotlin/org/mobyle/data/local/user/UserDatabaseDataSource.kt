package org.mobyle.data.local.user

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mobyle.data.local.database.MoviesTable
import org.mobyle.data.local.database.UserMoviesTable
import org.mobyle.data.local.database.UsersTable
import org.mobyle.domain.model.Movie
import org.mobyle.domain.model.User

interface UserDatabaseDataSource {
    fun findByEmail(email: String): User?
    fun save(user: User): User
    fun findByUsername(prefix: String): List<String>
    fun findRecentlyWatchedMovies(userExternalId: String, limit: Int = 10): List<Movie>
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

    override fun findRecentlyWatchedMovies(userExternalId: String, limit: Int): List<Movie> {
        return transaction {
            // Resolve internal DB id from external id
            val userDbId = UsersTable.selectAll()
                .where { UsersTable.externalId eq userExternalId }
                .firstOrNull()
                ?.get(UsersTable.id)?.value ?: return@transaction emptyList()

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
                        posterPath = row[MoviesTable.posterPath]
                    )
                }
        }
    }
}
