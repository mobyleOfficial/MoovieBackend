package com.moovie.backend.domain.usecase.activities;

import com.moovie.backend.domain.model.MovieReviewDraft;
import com.moovie.backend.domain.repository.UserActivitiesRepository;
import org.springframework.stereotype.Service;

@Service
public class SubmitReview {

    private final UserActivitiesRepository userActivitiesRepository;

    public SubmitReview(UserActivitiesRepository userActivitiesRepository) {
        this.userActivitiesRepository = userActivitiesRepository;
    }

    public void invoke(MovieReviewDraft draft) {
        userActivitiesRepository.submitReview(draft);
    }
}
