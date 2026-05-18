package org.mobyle.data.local.user

import org.mobyle.domain.model.User
import java.util.concurrent.ConcurrentHashMap

interface UserLocalDataSource {
    suspend fun saveUser(user: User): Result<User>
    suspend fun getUserById(userId: String): Result<User?>
    suspend fun getUserByEmail(email: String): Result<User?>
}

class UserLocalDataSourceImpl : UserLocalDataSource {
    private val users = ConcurrentHashMap<String, User>()

    override suspend fun saveUser(user: User): Result<User> {
        return try {
            users[user.id] = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserById(userId: String): Result<User?> {
        return try {
            Result.success(users[userId])
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserByEmail(email: String): Result<User?> {
        return try {
            val user = users.values.find { it.email == email }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
