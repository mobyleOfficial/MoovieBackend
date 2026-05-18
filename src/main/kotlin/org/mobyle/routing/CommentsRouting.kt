package org.mobyle.routing

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.mobyle.di.injection
import org.mobyle.domain.usecase.GetCommentsUseCase

fun Route.getCommentsRouting() {
    val getComments by injection<GetCommentsUseCase>()

    get("/comments/{contentId}") {
        val contentId = call.parameters["contentId"] ?: ""
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

        val comments = getComments(contentId, page, pageSize)
        call.respond(comments)
    }
}
