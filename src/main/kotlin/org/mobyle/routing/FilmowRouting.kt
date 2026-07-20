package org.mobyle.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.mobyle.di.injection
import org.mobyle.domain.model.FilmowScrapeRequest
import org.mobyle.domain.usecase.filmow.ScrapeFilmowProfile

fun Route.getFilmowRouting() {
    val scrapeFilmowProfile by injection<ScrapeFilmowProfile>()

    post("/filmow/scrape") {
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
            val profile = scrapeFilmowProfile(request.cookies, request.username)
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
