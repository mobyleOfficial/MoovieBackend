package org.mobyle.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import org.mobyle.di.injection
import org.mobyle.domain.model.UserProfile
import org.mobyle.domain.usecase.profile.GetPublicProfile
import org.mobyle.domain.usecase.profile.GetUserProfile
import org.mobyle.domain.usecase.profile.UpdateUserProfile

fun Route.getProfileRouting() {
    val getUserProfile by injection<GetUserProfile>()
    val updateUserProfile by injection<UpdateUserProfile>()
    val getPublicProfile by injection<GetPublicProfile>()

    get("/profile") {
        call.respond(getUserProfile())
    }

    put("/profile") {
        val profile = call.receive<UserProfile>()
        updateUserProfile(profile)
        call.respond(HttpStatusCode.OK)
    }

    get("/profile/{userId}") {
        val userId = call.parameters["userId"]
        if (userId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "User ID is required")
            return@get
        }
        call.respond(getPublicProfile(userId))
    }
}
