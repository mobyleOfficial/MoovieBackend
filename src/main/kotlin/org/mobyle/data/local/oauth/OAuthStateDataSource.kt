package org.mobyle.data.local.oauth

interface OAuthStateDataSource {
    suspend fun saveState(state: String, expiresAt: Long): Result<Unit>
    suspend fun validateAndRemoveState(state: String): Result<Boolean>
}

data class StateRecord(val expiresAt: Long)

class OAuthStateDataSourceImpl : OAuthStateDataSource {
    private val states = java.util.concurrent.ConcurrentHashMap<String, StateRecord>()

    override suspend fun saveState(state: String, expiresAt: Long): Result<Unit> {
        return try {
            states[state] = StateRecord(expiresAt)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateAndRemoveState(state: String): Result<Boolean> {
        return try {
            val record = states[state]
            if (record == null) {
                Result.success(false)
                return Result.success(false)
            }

            val now = System.currentTimeMillis() / 1000
            if (record.expiresAt < now) {
                states.remove(state)
                return Result.success(false)
            }

            states.remove(state)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
