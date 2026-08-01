package org.mobyle.data.service

import java.util.concurrent.ConcurrentHashMap

class ScrapeStatusManager {
    private val scrapingUsers = ConcurrentHashMap<String, Long>()

    fun markScraping(userId: String) {
        scrapingUsers[userId] = System.currentTimeMillis()
    }

    fun clearScraping(userId: String) {
        scrapingUsers.remove(userId)
    }

    fun isScraping(userId: String): Boolean {
        return scrapingUsers.containsKey(userId)
    }
}
