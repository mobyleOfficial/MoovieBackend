package org.mobyle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String,
    val authorName: String,
    val authorAvatar: String,
    val content: String,
    val createdAt: String,
    val rating: Double
)

@Serializable
data class CommentListing(
    val contentId: String,
    val totalPages: Int,
    val totalResults: Int,
    val page: Int,
    val comments: List<Comment>
)
