package com.moovie.backend.data.repository;

import com.moovie.backend.data.remote.comments.CommentsDataSource;
import com.moovie.backend.domain.repository.CommentsRepository;
import com.moovie.backend.domain.model.CommentListing;
import org.springframework.stereotype.Repository;

@Repository
public class CommentsRepositoryImpl implements CommentsRepository {

    private final CommentsDataSource commentsDataSource;

    public CommentsRepositoryImpl(CommentsDataSource commentsDataSource) {
        this.commentsDataSource = commentsDataSource;
    }

    @Override
    public CommentListing getComments(String contentId, int page, int pageSize) {
        return commentsDataSource.getComments(contentId, page, pageSize);
    }
}
