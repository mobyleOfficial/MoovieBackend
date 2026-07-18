package org.mobyle.domain.usecase.articles

import org.mobyle.domain.model.Article
import org.mobyle.domain.repository.ArticlesRepository

class GetArticleDetail(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(articleId: Long): Article? {
        require(articleId > 0) { "Article ID must be greater than 0" }

        return repository.getArticleById(articleId)
    }
}
