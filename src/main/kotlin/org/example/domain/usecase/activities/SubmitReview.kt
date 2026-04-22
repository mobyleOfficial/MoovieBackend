package org.example.domain.usecase.activities

import kotlinx.coroutines.runBlocking
import org.example.domain.model.MovieReviewDraft
import org.example.domain.repository.UserActivitiesRepository

class SubmitReview(private val repository: UserActivitiesRepository) {
    operator fun invoke(draft: MovieReviewDraft) = runBlocking {
        repository.submitReview(draft)
    }
}
