package org.mobyle.data.repository

import org.mobyle.domain.model.MovieReviewDraft
import org.mobyle.domain.model.UserActivity
import org.mobyle.domain.repository.UserActivitiesRepository
import org.mobyle.model.UserActivityListing

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
