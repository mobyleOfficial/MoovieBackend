package org.mobyle.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.mobyle.di.injection
import org.mobyle.domain.usecase.articles.GetArticleDetail
import org.mobyle.domain.usecase.articles.GetArticles

fun Route.getArticlesRouting() {
    val getArticles by injection<GetArticles>()
    val getArticleDetail by injection<GetArticleDetail>()

    get("/articles") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

        val articles = getArticles(page, pageSize)
        call.respond(articles)
    }

    get("/articles/{articleId}") {
        val articleId = call.parameters["articleId"] ?: ""
        val article = getArticleDetail(articleId)

        if (article != null) {
            call.respond(article)
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Article not found"))
        }
    }
}
