package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Language(
    val iso: String,
    val englishName: String
)
