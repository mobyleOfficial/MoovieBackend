package org.mobyle.data.remote.articles

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.mobyle.data.local.database.ArticlesTable
import org.mobyle.domain.model.Article
import org.mobyle.domain.model.ArticleListing
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

class ArticlesDataSourceImpl : ArticlesDataSource {

    private val log = LoggerFactory.getLogger(ArticlesDataSourceImpl::class.java)

    companion object {
        private val RSS_FEEDS = listOf(
            RssFeed(
                url = "https://g1.globo.com/rss/g1/pop-arte/cinema/",
                source = "G1 Cinema"
            )
        )
    }

    override suspend fun scrapeAndStore() {
        RSS_FEEDS.forEach { feed ->
            try {
                scrapeRssFeed(feed)
            } catch (e: Exception) {
                log.error("Failed to scrape RSS feed: ${feed.url}", e)
            }
        }
    }

    private fun scrapeRssFeed(feed: RssFeed) {
        val doc = Jsoup.connect(feed.url)
            .userAgent("MoovieAi/1.0")
            .timeout(15_000)
            .parser(Parser.xmlParser())
            .get()

        val items = doc.select("item")
        log.info("Found ${items.size} items from ${feed.source}")

        val now = Clock.System.now()

        transaction {
            for (item in items) {
                val title = item.selectFirst("title")?.text() ?: continue
                val link = item.selectFirst("link")?.text() ?: continue
                val subtitle = item.selectFirst("atom|subtitle")?.text() ?: ""
                val pubDateStr = item.selectFirst("pubDate")?.text()
                val imageUrl = item.selectFirst("media|content")?.attr("url")

                val publishedAt = parsePubDate(pubDateStr) ?: now

                ArticlesTable.insertIgnore {
                    it[sourceUrl] = link
                    it[ArticlesTable.title] = title
                    it[summary] = subtitle
                    it[ArticlesTable.imageUrl] = imageUrl?.takeIf { url -> url.isNotBlank() }
                    it[sourceName] = feed.source
                    it[ArticlesTable.publishedAt] = publishedAt
                    it[scrapedAt] = now
                }
            }
        }
    }

    private fun parsePubDate(dateStr: String?): Instant? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val zdt = ZonedDateTime.parse(dateStr, formatter)
            Instant.fromEpochSeconds(zdt.toEpochSecond())
        } catch (e: Exception) {
            log.warn("Failed to parse date: $dateStr", e)
            null
        }
    }

    override suspend fun getArticles(page: Int, pageSize: Int): ArticleListing {
        return transaction {
            val totalResults = ArticlesTable.selectAll().count().toInt()
            val totalPages = if (totalResults == 0) 0 else ceil(totalResults.toDouble() / pageSize).toInt()
            val offset = ((page - 1) * pageSize).toLong()

            val articles = ArticlesTable
                .selectAll()
                .orderBy(ArticlesTable.publishedAt, SortOrder.DESC)
                .limit(pageSize)
                .offset(offset)
                .map { row -> row.toArticle() }

            ArticleListing(
                totalPages = totalPages,
                totalResults = totalResults,
                page = page,
                articles = articles
            )
        }
    }

    override suspend fun getArticleById(articleId: Long): Article? {
        return transaction {
            ArticlesTable
                .selectAll()
                .where { ArticlesTable.id eq articleId }
                .firstOrNull()
                ?.toArticle()
        }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toArticle(): Article {
        return Article(
            id = this[ArticlesTable.id].value,
            title = this[ArticlesTable.title],
            summary = this[ArticlesTable.summary],
            imageUrl = this[ArticlesTable.imageUrl],
            sourceUrl = this[ArticlesTable.sourceUrl],
            source = this[ArticlesTable.sourceName],
            publishedAt = this[ArticlesTable.publishedAt].toString()
        )
    }

    private data class RssFeed(val url: String, val source: String)
}
