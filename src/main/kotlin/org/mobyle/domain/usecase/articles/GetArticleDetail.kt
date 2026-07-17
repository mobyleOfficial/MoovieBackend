package org.mobyle.domain.usecase.articles

import org.mobyle.domain.model.Article
import org.mobyle.domain.repository.ArticlesRepository

class GetArticleDetail(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(articleId: String): Article? {
        require(articleId.isNotBlank()) { "Article ID cannot be blank" }

        return repository.getArticleById(articleId)
    }
}
