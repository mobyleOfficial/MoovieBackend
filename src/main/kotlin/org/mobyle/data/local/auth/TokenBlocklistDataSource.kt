package org.mobyle.data.local.auth

import java.util.concurrent.ConcurrentHashMap

class TokenBlocklistDataSource {
    private val blocklist = ConcurrentHashMap<String, Long>() // token -> expiresAt (unix seconds)

    fun revoke(token: String, expiresAt: Long) {
        blocklist[token] = expiresAt
    }

    fun isRevoked(token: String): Boolean {
        return blocklist.containsKey(token)
    }

    fun cleanup() {
        val now = System.currentTimeMillis() / 1000
        blocklist.entries.removeIf { it.value < now }
    }
}
