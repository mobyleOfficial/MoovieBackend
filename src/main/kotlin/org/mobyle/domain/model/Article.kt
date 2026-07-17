package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: Long,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val sourceUrl: String,
    val source: String,
    val publishedAt: String
)

@Serializable
data class ArticleListing(
    val totalPages: Int,
    val totalResults: Int,
    val page: Int,
    val articles: List<Article>
)
