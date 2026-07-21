package org.mobyle.data.remote.filmow

import kotlinx.serialization.Serializable

@Serializable
data class FilmowScrapeRequest(
    val cookies: String = "",
    val username: String
)
