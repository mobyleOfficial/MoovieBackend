package org.mobyle.domain.usecase

import org.mobyle.domain.model.CommentListing
import org.mobyle.domain.repository.CommentsRepository

class GetCommentsUseCase(
    private val commentsRepository: CommentsRepository
) {
    suspend operator fun invoke(contentId: String, page: Int = 1, pageSize: Int = 10): CommentListing {
        require(contentId.isNotBlank()) { "Content ID cannot be blank" }
        require(page > 0) { "Page must be greater than 0" }
        require(pageSize > 0) { "Page size must be greater than 0" }

        return commentsRepository.getComments(contentId, page, pageSize)
    }
}
