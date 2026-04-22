package org.example.domain.usecase.activities

import kotlinx.coroutines.runBlocking
import org.example.domain.repository.UserActivitiesRepository
import org.example.model.UserActivityListing

class GetFriendsActivities(private val repository: UserActivitiesRepository) {
    operator fun invoke(page: Int): UserActivityListing = runBlocking {
        repository.getFriendsActivities(page)
    }
}
