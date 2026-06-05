package com.moovie.backend.domain.usecase.activities;

import com.moovie.backend.domain.model.UserActivity;
import com.moovie.backend.domain.repository.UserActivitiesRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetUserActivities {

    private final UserActivitiesRepository userActivitiesRepository;

    public GetUserActivities(UserActivitiesRepository userActivitiesRepository) {
        this.userActivitiesRepository = userActivitiesRepository;
    }

    public List<UserActivity> invoke(String userId) {
        return userActivitiesRepository.getUserActivities(userId);
    }
}
