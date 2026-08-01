package org.mobyle.data.service

import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

data class WsTokenEntry(
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long
)

class WsTokenManager(
    private val tokenTtlMs: Long = 30_000 // nonce valid for 30 seconds
) {
    private val tokens = ConcurrentHashMap<String, WsTokenEntry>()
    private val random = SecureRandom()

    fun generateToken(userId: String): String {
        cleanup()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        tokens[nonce] = WsTokenEntry(
            userId = userId,
            expiresAt = System.currentTimeMillis() + tokenTtlMs
        )
        return nonce
    }

    fun validateAndConsume(nonce: String): String? {
        val entry = tokens.remove(nonce) ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) return null
        return entry.userId
    }

    private fun cleanup() {
        val now = System.currentTimeMillis()
        tokens.entries.removeIf { it.value.expiresAt < now }
    }
}
