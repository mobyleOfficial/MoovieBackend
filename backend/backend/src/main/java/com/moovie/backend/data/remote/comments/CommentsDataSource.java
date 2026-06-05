package com.moovie.backend.data.remote.comments;

import com.moovie.backend.domain.model.CommentListing;

public interface CommentsDataSource {

    CommentListing getComments(String contentId, int page, int pageSize);
}
