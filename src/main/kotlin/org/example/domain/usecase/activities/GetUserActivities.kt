package org.example.domain.usecase.activities

import kotlinx.coroutines.runBlocking
import org.example.domain.model.UserActivity
import org.example.domain.repository.UserActivitiesRepository

class GetUserActivities(private val repository: UserActivitiesRepository) {
    operator fun invoke(userId: String): List<UserActivity> = runBlocking {
        repository.getUserActivities(userId)
    }
}
