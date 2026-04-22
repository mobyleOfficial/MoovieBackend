package org.mobyle.domain.usecase.activities

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.repository.UserActivitiesRepository
import org.mobyle.model.UserActivityListing

class GetFriendsActivities(private val repository: UserActivitiesRepository) {
    operator fun invoke(page: Int): UserActivityListing = runBlocking {
        repository.getFriendsActivities(page)
    }
}
