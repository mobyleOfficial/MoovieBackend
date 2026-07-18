package org.mobyle

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.mobyle.data.local.database.DatabaseConfig
import org.mobyle.data.remote.articles.ArticlesDataSource
import org.koin.ktor.ext.inject
import org.mobyle.data.di.dataModule
import org.mobyle.di.appModule
import org.mobyle.routing.getActivitiesRouting
import org.mobyle.routing.getArticlesRouting
import org.mobyle.routing.getAuthRouting
import org.mobyle.routing.getCommentsRouting
import org.mobyle.routing.getMoviesRouting
import org.mobyle.routing.getProfileRouting
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Application")

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    logTmdbEnv()
    DatabaseConfig.init()

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        configureCors()
        configureStatusPages()
        configureKoin()
        configureRouting()
        scheduleArticleScraping()
    }.start(wait = true)
}

private fun Application.configureCors() {
    install(CORS) {
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        // Local dev
        allowHost("localhost:5173", schemes = listOf("http"))
        allowHost("127.0.0.1:5173", schemes = listOf("http"))
        // Extra origins from env
        System.getenv("CORS_ORIGINS")?.split(",")?.forEach { origin ->
            val trimmed = origin.trim().takeIf { it.isNotBlank() } ?: return@forEach
            val host = trimmed.removePrefix("http://").removePrefix("https://")
            val schemes = if (trimmed.startsWith("https")) listOf("https") else listOf("http", "https")
            allowHost(host, schemes = schemes)
        }
    }
}

private fun logTmdbEnv() {
    val apiKey = System.getenv("TMDB_API_KEY")
    when {
        apiKey.isNullOrBlank() -> log.warn("TMDB_API_KEY is not set - movie API routes will fail with 500")
        else -> log.info("TMDB API key configured")
    }
}

private fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError, mapOf(
                    "error" to "Internal Server Error",
                    "message" to (cause.message ?: "Unknown error")
                )
            )
        }
    }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(dataModule, appModule)
    }
}

private fun Application.scheduleArticleScraping() {
    val articlesDataSource by inject<ArticlesDataSource>()
    val intervalMs = System.getenv("SCRAPE_INTERVAL_HOURS")?.toLongOrNull()?.times(3_600_000) ?: 10_800_000L // default 3h

    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
        while (isActive) {
            try {
                log.info("Starting scheduled article scraping...")
                articlesDataSource.scrapeAndStore()
                log.info("Article scraping completed successfully")
            } catch (e: Exception) {
                log.error("Article scraping failed", e)
            }
            delay(intervalMs)
        }
    }
}

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    routing {
        getAuthRouting()
        getMoviesRouting()
        getProfileRouting()
        getActivitiesRouting()
        getCommentsRouting()
        getArticlesRouting()
    }
}
