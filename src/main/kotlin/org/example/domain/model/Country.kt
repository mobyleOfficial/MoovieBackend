package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val iso: String,
    val englishName: String
)
