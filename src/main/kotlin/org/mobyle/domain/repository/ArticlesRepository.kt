package org.mobyle.domain.repository

import org.mobyle.domain.model.Article
import org.mobyle.domain.model.ArticleListing

interface ArticlesRepository {
    suspend fun getArticles(page: Int, pageSize: Int = 10): ArticleListing
    suspend fun getArticleById(articleId: Long): Article?
}
