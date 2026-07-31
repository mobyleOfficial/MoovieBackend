package org.mobyle.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.mobyle.di.injection
import org.mobyle.data.remote.auth.authenticateJWT
import org.mobyle.data.remote.filmow.FilmowScrapeRequest
import org.mobyle.data.local.user.UserDatabaseDataSource
import org.mobyle.domain.usecase.auth.ValidateToken
import org.mobyle.domain.usecase.filmow.ImportFilmowData
import org.mobyle.domain.usecase.filmow.ScrapeFilmowProfile
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("FilmowRouting")

fun Route.getFilmowRouting() {
    val scrapeFilmowProfile by injection<ScrapeFilmowProfile>()
    val importFilmowData by injection<ImportFilmowData>()
    val validateToken by injection<ValidateToken>()
    val userDatabaseDataSource by injection<UserDatabaseDataSource>()

    post("/filmow/scrape") {
        val principal = call.authenticateJWT(validateToken) ?: return@post
        val userId = principal.claims.userId
        val email = principal.claims.email

        val user = userDatabaseDataSource.findByEmail(email)
        if (user == null) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "User not found")
            )
            return@post
        }

        val request = try {
            call.receive<FilmowScrapeRequest>()
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid request body. Expected: { cookies: string, username: string }")
            )
            return@post
        }

        if (request.username.isBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "'username' is required")
            )
            return@post
        }

        try {
            println("[FILMOW] >>> Endpoint /filmow/scrape called. user=$userId, filmowUser=@${request.username}")
            log.info("[FILMOW] Starting scrape for Filmow user @${request.username} (moovie user $userId)")
            val profile = scrapeFilmowProfile(request.cookies, request.username)
            println("[FILMOW] <<< Scrape returned. movies: watched=${profile.watched.size}, watchlist=${profile.watchlist.size}, favorites=${profile.favorites.size}, lists=${profile.lists.size}")
            log.info("[FILMOW] Scrape done. Starting import to DB...")

            try {
                importFilmowData(user.id, profile)
                log.info("[FILMOW] Import done.")
            } catch (e: Exception) {
                log.error("[FILMOW] Import failed for user $userId: ${e.message}", e)
            }

            log.info("[FILMOW] Sending response...")
            call.respond(profile)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to "Failed to scrape Filmow profile",
                    "message" to (e.message ?: "Unknown error")
                )
            )
        }
    }
}
