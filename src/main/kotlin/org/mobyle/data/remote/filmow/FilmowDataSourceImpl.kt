package org.mobyle.data.remote.filmow

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mobyle.domain.model.FilmowList
import org.mobyle.domain.model.FilmowProfile
import org.mobyle.domain.model.Movie
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

class FilmowDataSourceImpl(
    private val tmdbHttpClient: HttpClient
) : FilmowDataSource {

    private val log = LoggerFactory.getLogger(FilmowDataSourceImpl::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    companion object {
        private const val SCRIPT_PATH = "scripts/filmow_scraper.py"
        private const val VENV_PYTHON_WIN = "scripts/.venv/Scripts/python.exe"
        private const val VENV_PYTHON_UNIX = "scripts/.venv/bin/python"
        private const val TIMEOUT_MINUTES = 10L
    }

    override suspend fun scrapeProfile(cookies: String, username: String): FilmowProfile {
        val scriptFile = resolveScriptPath()
        if (!scriptFile.exists()) {
            throw IllegalStateException("Python scraper not found at: ${scriptFile.absolutePath}")
        }

        val pythonBin = resolveVenvPython()
        log.info("Using Python: $pythonBin")

        val command = mutableListOf(pythonBin, scriptFile.absolutePath, username)
        if (cookies.isNotBlank()) {
            command.add(cookies)
        }

        log.info("Starting Filmow scrape for @$username via Python sidecar")

        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        val exited = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)
        if (!exited) {
            process.destroyForcibly()
            throw RuntimeException("Filmow scraper timed out after $TIMEOUT_MINUTES minutes")
        }

        if (stderr.isNotBlank()) {
            log.info("Filmow scraper log:\n$stderr")
        }

        if (process.exitValue() != 0) {
            log.error("Filmow scraper failed (exit ${process.exitValue()}): $stdout")
            throw RuntimeException("Filmow scraper failed: $stdout")
        }

        return parseResult(stdout)
    }

    private suspend fun parseResult(jsonStr: String): FilmowProfile {
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        if (obj.containsKey("error")) {
            val errorMsg = obj["error"]?.jsonPrimitive?.content ?: "Unknown error"
            throw RuntimeException("Filmow scraper error: $errorMsg")
        }

        return FilmowProfile(
            username = obj["username"]?.jsonPrimitive?.content ?: "",
            displayName = obj["displayName"]?.jsonPrimitive?.content ?: "",
            watched = parseMovieList(obj["watched"]),
            watchlist = parseMovieList(obj["watchlist"]),
            favorites = parseMovieList(obj["favorites"]),
            watchedSeries = parseMovieList(obj["watchedSeries"]),
            watchlistSeries = parseMovieList(obj["watchlistSeries"]),
            lists = parseListList(obj["lists"]),
            errors = obj["errors"]?.jsonArray
                ?.map { it.jsonPrimitive.content }
                ?: emptyList()
        )
    }

    private suspend fun parseMovieList(element: kotlinx.serialization.json.JsonElement?): List<Movie> {
        if (element == null || element !is JsonArray) return emptyList()

        return element.mapNotNull { item ->
            try {
                val movie = item.jsonObject
                val imdbId = movie["imdbId"]?.jsonPrimitive?.content
                val tmdbId = if (imdbId != null) resolveTmdbId(imdbId) else null

                if (tmdbId == null) {
                    log.warn("Skipping movie without TMDB ID: ${movie["title"]?.jsonPrimitive?.content}")
                    return@mapNotNull null
                }

                Movie(
                    id = tmdbId,
                    title = movie["title"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    overview = movie["overview"]?.jsonPrimitive?.content ?: "",
                    posterPath = movie["posterUrl"]?.jsonPrimitive?.content,
                    backdropPath = movie["backdropUrl"]?.jsonPrimitive?.content,
                    voteAverage = movie["voteAverage"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    releaseDate = movie["releaseDate"]?.jsonPrimitive?.content
                )
            } catch (e: Exception) {
                log.warn("Failed to parse movie item: ${e.message}")
                null
            }
        }
    }

    /**
     * Resolves IMDB ID to TMDB ID using TMDB /find/{external_id} endpoint.
     */
    private suspend fun resolveTmdbId(imdbId: String): Int? {
        return try {
            val response = tmdbHttpClient.get("find/$imdbId") {
                parameter("external_source", "imdb_id")
            }
            val body = response.bodyAsText()
            val result = json.parseToJsonElement(body).jsonObject
            val movieResults = result["movie_results"]?.jsonArray
            movieResults?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull
        } catch (e: Exception) {
            log.debug("Failed to resolve TMDB ID for $imdbId: ${e.message}")
            null
        }
    }

    private suspend fun parseListList(element: kotlinx.serialization.json.JsonElement?): List<FilmowList> {
        if (element == null || element !is JsonArray) return emptyList()

        return element.mapNotNull { item ->
            try {
                val list = item.jsonObject
                FilmowList(
                    filmowId = list["filmowId"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    title = list["title"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    description = list["description"]?.jsonPrimitive?.content,
                    filmowUrl = list["filmowUrl"]?.jsonPrimitive?.content ?: "",
                    coverUrl = list["coverUrl"]?.jsonPrimitive?.content,
                    movies = parseMovieList(list["movies"])
                )
            } catch (e: Exception) {
                log.warn("Failed to parse list item: ${e.message}")
                null
            }
        }
    }

    private fun resolveVenvPython(): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val venvPath = if (isWindows) VENV_PYTHON_WIN else VENV_PYTHON_UNIX

        val venvFile = File(venvPath)
        if (venvFile.exists()) return venvFile.absolutePath

        // Fallback to system python
        return "python"
    }

    private fun resolveScriptPath(): File {
        // Try relative to working directory first
        val relative = File(SCRIPT_PATH)
        if (relative.exists()) return relative

        // Try relative to the jar location
        val jarDir = File(
            FilmowDataSourceImpl::class.java.protectionDomain.codeSource.location.toURI()
        ).parentFile
        val fromJar = File(jarDir, SCRIPT_PATH)
        if (fromJar.exists()) return fromJar

        return relative
    }
}
