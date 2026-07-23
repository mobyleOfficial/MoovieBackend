package org.mobyle.data.local.user

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mobyle.data.local.database.UserFollowsTable
import org.mobyle.data.local.database.UserMoviesTable
import org.mobyle.data.local.database.UsersTable
import org.mobyle.domain.model.User

interface UserDatabaseDataSource {
    fun findByEmail(email: String): User?
    fun save(user: User): User
    fun findByUsername(prefix: String): List<String>
    fun countWatchedMovies(userExternalId: String): Int
    fun countFollowing(userExternalId: String): Int
    fun countFollowers(userExternalId: String): Int
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
}
