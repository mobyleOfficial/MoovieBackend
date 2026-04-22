package org.example.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.example.di.injection
import org.example.domain.model.MovieReviewDraft
import org.example.domain.usecase.activities.GetFriendsActivities
import org.example.domain.usecase.activities.GetUserActivities
import org.example.domain.usecase.activities.SubmitReview

fun Route.getActivitiesRouting() {
    val getUserActivities by injection<GetUserActivities>()
    val getFriendsActivities by injection<GetFriendsActivities>()
    val submitReview by injection<SubmitReview>()

    get("/activities/{userId}") {
        val userId = call.parameters["userId"]
        if (userId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "User ID is required")
            return@get
        }
        call.respond(getUserActivities(userId))
    }

    get("/activities/friends") {
        val page = call.parameters["page"]?.toIntOrNull() ?: 1
        call.respond(getFriendsActivities(page))
    }

    post("/reviews") {
        val draft = call.receive<MovieReviewDraft>()
        submitReview(draft)
        call.respond(HttpStatusCode.Created)
    }
}
