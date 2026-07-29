package org.mobyle.data.remote.filmow

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.doubleOrNull
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

class FilmowDataSourceImpl : FilmowDataSource {

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

        log.info("[SCRAPE] Starting Python scraper for @$username")
        val startTime = System.currentTimeMillis()

        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        log.info("[SCRAPE] Python process started, reading stdout...")
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        log.info("[SCRAPE] Waiting for Python process to finish...")
        val exited = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)
        if (!exited) {
            process.destroyForcibly()
            throw RuntimeException("Filmow scraper timed out after $TIMEOUT_MINUTES minutes")
        }

        val scraperMs = System.currentTimeMillis() - startTime
        log.info("[SCRAPE] Python scraper finished in ${scraperMs / 1000}s (exit ${process.exitValue()}, stdout ${stdout.length} chars)")

        if (stderr.isNotBlank()) {
            log.info("[SCRAPE] Scraper stderr:\n$stderr")
        }

        if (process.exitValue() != 0) {
            log.error("[SCRAPE] Scraper failed: $stdout")
            throw RuntimeException("Filmow scraper failed: $stdout")
        }

        log.info("[SCRAPE] Parsing JSON result...")
        val profile = parseResult(stdout)
        val totalMs = System.currentTimeMillis() - startTime
        log.info("[SCRAPE] Done in ${totalMs / 1000}s — " +
            "recentlyWatched=${profile.recentlyWatched.size}, " +
            "watched=${profile.watched.size}, " +
            "watchlist=${profile.watchlist.size}, " +
            "favorites=${profile.favorites.size}, " +
            "lists=${profile.lists.size}")
        return profile
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
            watchedCount = obj["watchedCount"]?.jsonPrimitive?.intOrNull ?: 0,
            recentlyWatched = parseMovieListLight(obj["recentlyWatched"]),
            watched = parseMovieListLight(obj["watched"]),
            watchlist = parseMovieListLight(obj["watchlist"]),
            favorites = parseMovieListLight(obj["favorites"]),
            lists = parseListList(obj["lists"]),
            errors = obj["errors"]?.jsonArray
                ?.map { it.jsonPrimitive.content }
                ?: emptyList()
        )
    }

    /**
     * Parses movies from scraped JSON without external API calls.
     */
    private fun parseMovieListLight(element: kotlinx.serialization.json.JsonElement?): List<Movie> {
        if (element == null || element !is JsonArray) return emptyList()

        return element.mapNotNull { item ->
            try {
                val movie = item.jsonObject
                val rawTitle = movie["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val year = movie["year"]?.jsonPrimitive?.content
                val cleanTitle = rawTitle.replace(Regex("\\(\\d{4}\\)"), "").trim()

                val filmowId = movie["filmowId"]?.jsonPrimitive?.content
                val posterUrl = movie["posterUrl"]?.jsonPrimitive?.content
                val movieId = filmowId?.toIntOrNull()?.let { -it }
                    ?: -(cleanTitle + (year ?: "") + (posterUrl ?: "")).hashCode().and(Int.MAX_VALUE)

                Movie(
                    id = movieId,
                    title = cleanTitle,
                    localTitle = movie["localTitle"]?.jsonPrimitive?.content,
                    originalTitle = movie["originalTitle"]?.jsonPrimitive?.content,
                    overview = "",
                    posterPath = movie["posterUrl"]?.jsonPrimitive?.content,
                    voteAverage = movie["voteAverage"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    userRating = movie["userRating"]?.jsonPrimitive?.doubleOrNull,
                    filmowId = filmowId
                )
            } catch (e: Exception) {
                log.warn("Failed to parse movie item: ${e.message}")
                null
            }
        }
    }

    private fun parseListList(element: kotlinx.serialization.json.JsonElement?): List<FilmowList> {
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
                    movies = parseMovieListLight(list["movies"])
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

        return "python"
    }

    private fun resolveScriptPath(): File {
        val relative = File(SCRIPT_PATH)
        if (relative.exists()) return relative

        val jarDir = File(
            FilmowDataSourceImpl::class.java.protectionDomain.codeSource.location.toURI()
        ).parentFile
        val fromJar = File(jarDir, SCRIPT_PATH)
        if (fromJar.exists()) return fromJar

        return relative
    }
}
