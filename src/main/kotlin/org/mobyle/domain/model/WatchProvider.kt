package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WatchProvider(
    val name: String,
    val logoPath: String? = null
)
