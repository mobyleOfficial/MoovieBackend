package org.mobyle.data.remote.filmow

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
import org.mobyle.domain.model.FilmowMovie
import org.mobyle.domain.model.FilmowProfile
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

    private fun parseResult(jsonStr: String): FilmowProfile {
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
            errors = obj["errors"]?.jsonArray
                ?.map { it.jsonPrimitive.content }
                ?: emptyList()
        )
    }

    private fun parseMovieList(element: kotlinx.serialization.json.JsonElement?): List<FilmowMovie> {
        if (element == null || element !is JsonArray) return emptyList()

        return element.mapNotNull { item ->
            try {
                val movie = item.jsonObject
                FilmowMovie(
                    filmowId = movie["filmowId"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    title = movie["title"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    year = movie["year"]?.jsonPrimitive?.content,
                    filmowUrl = movie["filmowUrl"]?.jsonPrimitive?.content ?: "",
                    posterUrl = movie["posterUrl"]?.jsonPrimitive?.content,
                    globalRating = movie["globalRating"]?.jsonPrimitive?.doubleOrNull,
                    userRating = movie["userRating"]?.jsonPrimitive?.intOrNull,
                    status = movie["status"]?.jsonPrimitive?.content ?: "",
                    director = movie["director"]?.jsonPrimitive?.content
                )
            } catch (e: Exception) {
                log.warn("Failed to parse movie item: ${e.message}")
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
