package org.mobyle.data.remote.articles

import org.mobyle.domain.model.Article
import org.mobyle.domain.model.ArticleListing

interface ArticlesDataSource {
    suspend fun getArticles(page: Int, pageSize: Int): ArticleListing
    suspend fun getArticleById(articleId: String): Article?
}
