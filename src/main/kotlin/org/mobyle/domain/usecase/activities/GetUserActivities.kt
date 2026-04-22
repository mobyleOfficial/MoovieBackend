package org.mobyle.domain.usecase.activities

import kotlinx.coroutines.runBlocking
import org.mobyle.domain.model.UserActivity
import org.mobyle.domain.repository.UserActivitiesRepository

class GetUserActivities(private val repository: UserActivitiesRepository) {
    operator fun invoke(userId: String): List<UserActivity> = runBlocking {
        repository.getUserActivities(userId)
    }
}
