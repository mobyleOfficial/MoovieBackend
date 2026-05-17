package org.mobyle.data.remote

import org.mobyle.domain.model.CommentListing

interface CommentsDataSource {
    suspend fun getComments(contentId: String, page: Int, pageSize: Int): CommentListing
}
