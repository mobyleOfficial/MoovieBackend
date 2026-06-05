package com.moovie.backend.domain.repository;

import com.moovie.backend.domain.model.MovieReviewDraft;
import com.moovie.backend.domain.model.UserActivity;
import com.moovie.backend.domain.model.UserActivityListing;

import java.util.List;

public interface UserActivitiesRepository {

    List<UserActivity> getUserActivities(String userId);

    UserActivityListing getFriendsActivities(int page);

    void submitReview(MovieReviewDraft draft);
}
