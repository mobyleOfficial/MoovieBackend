package org.mobyle.data.repository

import org.mobyle.data.remote.CommentsDataSource
import org.mobyle.domain.model.CommentListing
import org.mobyle.domain.repository.CommentsRepository

class CommentsRepositoryImpl(
    private val commentsDataSource: CommentsDataSource
) : CommentsRepository {

    override suspend fun getComments(contentId: String, page: Int, pageSize: Int): CommentListing {
        return commentsDataSource.getComments(contentId, page, pageSize)
    }
}
