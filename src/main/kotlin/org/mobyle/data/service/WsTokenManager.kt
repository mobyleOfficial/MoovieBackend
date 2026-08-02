package org.mobyle.data.service

import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(WsTokenManager::class.java)
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
        log.info("[WS-Token] Generated nonce for user $userId (TTL=${tokenTtlMs}ms, active tokens=${tokens.size})")
        return nonce
    }

    fun validateAndConsume(nonce: String): String? {
        val entry = tokens.remove(nonce)
        if (entry == null) {
            log.warn("[WS-Token] Nonce not found or already consumed")
            return null
        }
        if (System.currentTimeMillis() > entry.expiresAt) {
            log.warn("[WS-Token] Nonce expired for user ${entry.userId} (expired ${System.currentTimeMillis() - entry.expiresAt}ms ago)")
            return null
        }
        log.info("[WS-Token] Nonce validated and consumed for user ${entry.userId}")
        return entry.userId
    }

    private fun cleanup() {
        val now = System.currentTimeMillis()
        val before = tokens.size
        tokens.entries.removeIf { it.value.expiresAt < now }
        val removed = before - tokens.size
        if (removed > 0) {
            log.debug("[WS-Token] Cleanup removed $removed expired tokens (remaining=${tokens.size})")
        }
    }
}
