package org.mobyle.data.remote.filmow

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class FilmowDataSourceImpl(
    private val tmdbHttpClient: HttpClient
) : FilmowDataSource {

    private val log = LoggerFactory.getLogger(FilmowDataSourceImpl::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val tmdbIdCache = ConcurrentHashMap<String, Int?>()

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

        // Resolve TMDB IDs in parallel for all sections except lists
        return coroutineScope {
            val recentlyWatchedDeferred = async { parseMovieListWithTmdb(obj["recentlyWatched"]) }
            val watchedDeferred = async { parseMovieListWithTmdb(obj["watched"]) }
            val watchlistDeferred = async { parseMovieListWithTmdb(obj["watchlist"]) }
            val favoritesDeferred = async { parseMovieListWithTmdb(obj["favorites"]) }

            FilmowProfile(
                username = obj["username"]?.jsonPrimitive?.content ?: "",
                displayName = obj["displayName"]?.jsonPrimitive?.content ?: "",
                recentlyWatched = recentlyWatchedDeferred.await(),
                watched = watchedDeferred.await(),
                watchlist = watchlistDeferred.await(),
                favorites = favoritesDeferred.await(),
                lists = parseListList(obj["lists"]),
                errors = obj["errors"]?.jsonArray
                    ?.map { it.jsonPrimitive.content }
                    ?: emptyList()
            )
        }
    }

    /**
     * Parses movies and resolves TMDB IDs via search (title+year) in parallel.
     * Used for watched, watchlist, favorites, recentlyWatched.
     */
    private suspend fun parseMovieListWithTmdb(element: kotlinx.serialization.json.JsonElement?): List<Movie> {
        if (element == null || element !is JsonArray) return emptyList()

        data class ParsedItem(
            val title: String,
            val year: String?,
            val posterPath: String?,
            val voteAverage: Double
        )

        val parsed = element.mapNotNull { item ->
            try {
                val movie = item.jsonObject
                val rawTitle = movie["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val year = movie["year"]?.jsonPrimitive?.content
                val cleanTitle = rawTitle.replace(Regex("\\(\\d{4}\\)"), "").trim()
                ParsedItem(cleanTitle, year, movie["posterUrl"]?.jsonPrimitive?.content, movie["voteAverage"]?.jsonPrimitive?.doubleOrNull ?: 0.0)
            } catch (e: Exception) {
                log.warn("Failed to parse movie item: ${e.message}")
                null
            }
        }

        return coroutineScope {
            parsed.map { item ->
                async {
                    val tmdbId = searchTmdbId(item.title, item.year)
                    if (tmdbId == null) {
                        log.warn("Movie not found on TMDB: '${item.title}' (${item.year})")
                        return@async null
                    }
                    Movie(
                        id = tmdbId,
                        title = item.title,
                        overview = "",
                        posterPath = item.posterPath,
                        voteAverage = item.voteAverage,
                        releaseDate = item.year?.let { "$it-01-01" }
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    /**
     * Parses movies without resolving TMDB IDs. Used for list movies.
     */
    private fun parseMovieListLight(element: kotlinx.serialization.json.JsonElement?): List<Movie> {
        if (element == null || element !is JsonArray) return emptyList()

        return element.mapNotNull { item ->
            try {
                val movie = item.jsonObject
                val rawTitle = movie["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val year = movie["year"]?.jsonPrimitive?.content
                val cleanTitle = rawTitle.replace(Regex("\\(\\d{4}\\)"), "").trim()

                Movie(
                    id = 0,
                    title = cleanTitle,
                    overview = "",
                    posterPath = movie["posterUrl"]?.jsonPrimitive?.content,
                    voteAverage = movie["voteAverage"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    releaseDate = year?.let { "$it-01-01" }
                )
            } catch (e: Exception) {
                log.warn("Failed to parse movie item: ${e.message}")
                null
            }
        }
    }

    /**
     * Searches TMDB by title + year. Cached to avoid duplicate requests.
     */
    private suspend fun searchTmdbId(title: String, year: String?): Int? {
        val cacheKey = "$title|$year"
        tmdbIdCache[cacheKey]?.let { return it }

        return try {
            val response = tmdbHttpClient.get("search/movie") {
                parameter("query", title)
                if (year != null) parameter("year", year)
            }
            val body = response.bodyAsText()
            val result = json.parseToJsonElement(body).jsonObject
            val results = result["results"]?.jsonArray
            val tmdbId = results?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull
            tmdbIdCache[cacheKey] = tmdbId
            tmdbId
        } catch (e: Exception) {
            log.debug("Failed to search TMDB for '$title' ($year): ${e.message}")
            null
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
