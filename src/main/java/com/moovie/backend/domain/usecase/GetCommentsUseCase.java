package com.moovie.backend.domain.usecase;

import com.moovie.backend.domain.repository.CommentsRepository;
import com.moovie.backend.domain.model.CommentListing;
import org.springframework.stereotype.Service;

@Service
public class GetCommentsUseCase {

    private final CommentsRepository commentsRepository;

    public GetCommentsUseCase(CommentsRepository commentsRepository) {
        this.commentsRepository = commentsRepository;
    }

    public CommentListing invoke(String contentId, int page, int pageSize) {
        if (contentId == null || contentId.isBlank()) {
            throw new IllegalArgumentException("contentId must not be blank");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        return commentsRepository.getComments(contentId, page, pageSize);
    }
}
