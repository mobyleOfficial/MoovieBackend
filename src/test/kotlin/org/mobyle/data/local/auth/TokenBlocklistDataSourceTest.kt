package org.mobyle.data.local.auth

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.mobyle.data.local.database.TokenBlocklistTable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenBlocklistDataSourceTest {

    private lateinit var blocklist: TokenBlocklistDataSource

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:test_blocklist;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(TokenBlocklistTable) }
        blocklist = TokenBlocklistDataSource()
    }

    @AfterTest
    fun tearDown() {
        transaction { SchemaUtils.drop(TokenBlocklistTable) }
    }

    @Test
    fun `revoke adds token to blocklist`() {
        val token = "test-token-123"
        val expiresAt = System.currentTimeMillis() / 1000 + 3600

        blocklist.revoke(token, expiresAt)

        assertTrue(blocklist.isRevoked(token))
    }

    @Test
    fun `isRevoked returns false for unknown token`() {
        assertFalse(blocklist.isRevoked("unknown-token"))
    }

    @Test
    fun `cleanup removes expired tokens`() {
        val expiredToken = "expired-token"
        val validToken = "valid-token"

        blocklist.revoke(expiredToken, System.currentTimeMillis() / 1000 - 100)
        blocklist.revoke(validToken, System.currentTimeMillis() / 1000 + 3600)

        blocklist.cleanup()

        assertFalse(blocklist.isRevoked(expiredToken))
        assertTrue(blocklist.isRevoked(validToken))
    }

    @Test
    fun `cleanup with empty blocklist does not throw`() {
        blocklist.cleanup()
    }

    @Test
    fun `revoking same token twice overwrites expiry`() {
        val token = "test-token"

        blocklist.revoke(token, 100L)
        blocklist.revoke(token, System.currentTimeMillis() / 1000 + 3600)

        assertTrue(blocklist.isRevoked(token))

        blocklist.cleanup()
        assertTrue(blocklist.isRevoked(token))
    }
}
