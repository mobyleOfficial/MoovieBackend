package org.mobyle.data.local.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenBlocklistDataSourceTest {

    @Test
    fun `revoke adds token to blocklist`() {
        val blocklist = TokenBlocklistDataSource()
        val token = "test-token-123"
        val expiresAt = System.currentTimeMillis() / 1000 + 3600

        blocklist.revoke(token, expiresAt)

        assertTrue(blocklist.isRevoked(token))
    }

    @Test
    fun `isRevoked returns false for unknown token`() {
        val blocklist = TokenBlocklistDataSource()

        assertFalse(blocklist.isRevoked("unknown-token"))
    }

    @Test
    fun `cleanup removes expired tokens`() {
        val blocklist = TokenBlocklistDataSource()
        val expiredToken = "expired-token"
        val validToken = "valid-token"

        // Token that expired in the past
        blocklist.revoke(expiredToken, System.currentTimeMillis() / 1000 - 100)
        // Token that expires in the future
        blocklist.revoke(validToken, System.currentTimeMillis() / 1000 + 3600)

        blocklist.cleanup()

        assertFalse(blocklist.isRevoked(expiredToken))
        assertTrue(blocklist.isRevoked(validToken))
    }

    @Test
    fun `cleanup with empty blocklist does not throw`() {
        val blocklist = TokenBlocklistDataSource()
        blocklist.cleanup() // should not throw
    }

    @Test
    fun `revoking same token twice overwrites expiry`() {
        val blocklist = TokenBlocklistDataSource()
        val token = "test-token"

        blocklist.revoke(token, 100L)
        blocklist.revoke(token, System.currentTimeMillis() / 1000 + 3600)

        assertTrue(blocklist.isRevoked(token))

        // After cleanup, it should still be there since we updated expiry to future
        blocklist.cleanup()
        assertTrue(blocklist.isRevoked(token))
    }
}
