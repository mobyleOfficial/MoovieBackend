package com.moovie.backend.domain.repository;

import com.moovie.backend.domain.model.CommentListing;

public interface CommentsRepository {

    CommentListing getComments(String contentId, int page, int pageSize);
}
