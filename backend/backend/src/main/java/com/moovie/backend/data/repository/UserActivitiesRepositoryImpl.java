package com.moovie.backend.data.repository;

import com.moovie.backend.domain.model.MovieReviewDraft;
import com.moovie.backend.domain.model.UserActivity;
import com.moovie.backend.domain.repository.UserActivitiesRepository;
import com.moovie.backend.domain.model.UserActivityListing;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserActivitiesRepositoryImpl implements UserActivitiesRepository {

    private static final List<UserActivity> MOCK_ACTIVITIES = List.of(
            new UserActivity("Alice", "reviewed", "Fight Club", "2 hours ago"),
            new UserActivity("Bob", "added to watchlist", "The Godfather", "5 hours ago"),
            new UserActivity("Carol", "liked", "Pulp Fiction", "1 day ago"),
            new UserActivity("Dave", "reviewed", "Inception", "1 day ago"),
            new UserActivity("Eve", "added to list", "The Matrix", "2 days ago"),
            new UserActivity("Frank", "reviewed", "Interstellar", "3 days ago"),
            new UserActivity("Grace", "liked", "The Dark Knight", "3 days ago"),
            new UserActivity("Hank", "reviewed", "Parasite", "4 days ago")
    );

    @Override
    public List<UserActivity> getUserActivities(String userId) {
        return MOCK_ACTIVITIES.stream()
                .limit(5)
                .toList();
    }

    @Override
    public UserActivityListing getFriendsActivities(int page) {
        int pageSize = 5;
        int totalResults = MOCK_ACTIVITIES.size();
        int totalPages = (int) Math.ceil((double) totalResults / pageSize);

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalResults);

        List<UserActivity> pageActivities = (fromIndex < totalResults)
                ? MOCK_ACTIVITIES.subList(fromIndex, toIndex)
                : List.of();

        return new UserActivityListing(totalPages, totalResults, pageActivities);
    }

    @Override
    public void submitReview(MovieReviewDraft draft) {
        // TODO: persist to database
    }
}
