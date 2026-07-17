package org.mobyle.domain.usecase.articles

import org.mobyle.domain.model.ArticleListing
import org.mobyle.domain.repository.ArticlesRepository

class GetArticles(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 10): ArticleListing {
        require(page > 0) { "Page must be greater than 0" }
        require(pageSize > 0) { "Page size must be greater than 0" }

        return repository.getArticles(page, pageSize)
    }
}
