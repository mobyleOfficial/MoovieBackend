package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val authorName: String,
    val authorAvatar: String,
    val imageUrl: String,
    val category: String,
    val tags: List<String>,
    val publishedAt: String,
    val readTimeMinutes: Int
)

@Serializable
data class ArticleListing(
    val totalPages: Int,
    val totalResults: Int,
    val page: Int,
    val articles: List<Article>
)
