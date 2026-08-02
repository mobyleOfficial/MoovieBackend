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
        println("[WS-Token] Generated nonce for user $userId (TTL=${tokenTtlMs}ms, active tokens=${tokens.size})")
        return nonce
    }

    fun validateAndConsume(nonce: String): String? {
        val entry = tokens.remove(nonce)
        if (entry == null) {
            println("[WS-Token] Nonce not found or already consumed")
            return null
        }
        if (System.currentTimeMillis() > entry.expiresAt) {
            println("[WS-Token] Nonce expired for user ${entry.userId} (expired ${System.currentTimeMillis() - entry.expiresAt}ms ago)")
            return null
        }
        println("[WS-Token] Nonce validated and consumed for user ${entry.userId}")
        return entry.userId
    }

    private fun cleanup() {
        val now = System.currentTimeMillis()
        val before = tokens.size
        tokens.entries.removeIf { it.value.expiresAt < now }
        val removed = before - tokens.size
        if (removed > 0) {
            println("[WS-Token] Cleanup removed $removed expired tokens (remaining=${tokens.size})")
        }
    }
}
