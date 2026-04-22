package org.example.data.repository

import org.example.domain.model.MovieReviewDraft
import org.example.domain.model.UserActivity
import org.example.domain.repository.UserActivitiesRepository
import org.example.model.UserActivityListing

class UserActivitiesRepositoryImpl : UserActivitiesRepository {

    override suspend fun getUserActivities(userId: String): List<UserActivity> {
        // TODO: Replace with actual user data storage
        return emptyList()
    }

    override suspend fun getFriendsActivities(page: Int): UserActivityListing {
        // TODO: Replace with actual user data storage
        return UserActivityListing(totalPages = 0, totalResults = 0, activities = emptyList())
    }

    override suspend fun submitReview(draft: MovieReviewDraft) {
        // TODO: Replace with actual user data storage
    }
}
