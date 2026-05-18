package org.mobyle.domain.repository

import org.mobyle.domain.model.CommentListing

interface CommentsRepository {
    suspend fun getComments(contentId: String, page: Int, pageSize: Int = 10): CommentListing
}
