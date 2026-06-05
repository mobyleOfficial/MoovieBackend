package com.moovie.backend.core.controller;

import com.moovie.backend.domain.model.MovieReviewDraft;
import com.moovie.backend.domain.model.UserActivity;
import com.moovie.backend.domain.usecase.activities.GetFriendsActivities;
import com.moovie.backend.domain.usecase.activities.GetUserActivities;
import com.moovie.backend.domain.usecase.activities.SubmitReview;
import com.moovie.backend.domain.model.UserActivityListing;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ActivitiesController {

    private final GetUserActivities getUserActivities;
    private final GetFriendsActivities getFriendsActivities;
    private final SubmitReview submitReview;

    public ActivitiesController(GetUserActivities getUserActivities,
                                GetFriendsActivities getFriendsActivities,
                                SubmitReview submitReview) {
        this.getUserActivities = getUserActivities;
        this.getFriendsActivities = getFriendsActivities;
        this.submitReview = submitReview;
    }

    @GetMapping("/activities/{userId}")
    public List<UserActivity> userActivities(@PathVariable String userId) {
        return getUserActivities.invoke(userId);
    }

    @GetMapping("/activities/friends")
    public UserActivityListing friendsActivities(@RequestParam(defaultValue = "1") int page) {
        return getFriendsActivities.invoke(page);
    }

    @PostMapping("/reviews")
    public void submitReview(@RequestBody MovieReviewDraft draft) {
        submitReview.invoke(draft);
    }
}
