package org.example.domain.repository

import org.example.domain.model.MovieReviewDraft
import org.example.domain.model.UserActivity
import org.example.model.UserActivityListing

interface UserActivitiesRepository {
    suspend fun getUserActivities(userId: String): List<UserActivity>
    suspend fun getFriendsActivities(page: Int): UserActivityListing
    suspend fun submitReview(draft: MovieReviewDraft)
}
