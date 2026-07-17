package org.mobyle.data.repository

import org.mobyle.data.remote.articles.ArticlesDataSource
import org.mobyle.domain.model.Article
import org.mobyle.domain.model.ArticleListing
import org.mobyle.domain.repository.ArticlesRepository

class ArticlesRepositoryImpl(
    private val articlesDataSource: ArticlesDataSource
) : ArticlesRepository {

    override suspend fun getArticles(page: Int, pageSize: Int): ArticleListing {
        return articlesDataSource.getArticles(page, pageSize)
    }

    override suspend fun getArticleById(articleId: String): Article? {
        return articlesDataSource.getArticleById(articleId)
    }
}
