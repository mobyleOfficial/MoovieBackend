package com.moovie.backend.domain.usecase.activities;

import com.moovie.backend.domain.repository.UserActivitiesRepository;
import com.moovie.backend.domain.model.UserActivityListing;
import org.springframework.stereotype.Service;

@Service
public class GetFriendsActivities {

    private final UserActivitiesRepository userActivitiesRepository;

    public GetFriendsActivities(UserActivitiesRepository userActivitiesRepository) {
        this.userActivitiesRepository = userActivitiesRepository;
    }

    public UserActivityListing invoke(int page) {
        return userActivitiesRepository.getFriendsActivities(page);
    }
}
