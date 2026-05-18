package org.mobyle.data.repository

import org.mobyle.data.local.user.UserLocalDataSource
import org.mobyle.domain.model.User
import org.mobyle.domain.repository.UserRepository

class UserRepositoryImpl(
    private val userLocalDataSource: UserLocalDataSource
) : UserRepository {

    override suspend fun createOrUpdateUser(user: User): Result<User> {
        return userLocalDataSource.saveUser(user)
    }

    override suspend fun getUserByEmail(email: String): Result<User?> {
        return userLocalDataSource.getUserByEmail(email)
    }

    override suspend fun getUserById(userId: String): Result<User?> {
        return userLocalDataSource.getUserById(userId)
    }
}
