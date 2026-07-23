package org.mobyle.data.local.auth

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.mobyle.data.local.database.TokenBlocklistTable

class TokenBlocklistDataSource {

    fun revoke(token: String, expiresAt: Long) {
        transaction {
            TokenBlocklistTable.upsert(TokenBlocklistTable.token) {
                it[TokenBlocklistTable.token] = token
                it[TokenBlocklistTable.expiresAt] = expiresAt
            }
        }
    }

    fun isRevoked(token: String): Boolean {
        return transaction {
            TokenBlocklistTable.selectAll()
                .where { TokenBlocklistTable.token eq token }
                .count() > 0
        }
    }

    fun cleanup() {
        val now = System.currentTimeMillis() / 1000
        transaction {
            TokenBlocklistTable.deleteWhere { expiresAt less now }
        }
    }
}
