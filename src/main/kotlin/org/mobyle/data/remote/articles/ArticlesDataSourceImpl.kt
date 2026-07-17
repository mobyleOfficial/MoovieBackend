package org.mobyle.data.remote.articles

import org.mobyle.domain.model.Article
import org.mobyle.domain.model.ArticleListing
import kotlin.math.ceil

class ArticlesDataSourceImpl : ArticlesDataSource {

    companion object {
        private fun getMockArticles(): List<Article> = listOf(
            Article(
                id = "article-1",
                title = "The Rise of Practical Effects in Modern Cinema",
                summary = "How filmmakers are returning to practical effects to create more immersive movie experiences.",
                content = "In an era dominated by CGI, a growing number of directors are rediscovering the magic of practical effects. From Christopher Nolan's insistence on real stunts in the Mission: Impossible franchise to the intricate miniatures used in recent sci-fi films, practical effects are making a powerful comeback. This shift is driven by audiences who crave authenticity and tactile realism that digital effects sometimes struggle to deliver.",
                authorName = "Elena Vasquez",
                authorAvatar = "https://api.example.com/avatars/elena-vasquez.jpg",
                imageUrl = "https://api.example.com/images/practical-effects.jpg",
                category = "Film Industry",
                tags = listOf("practical effects", "CGI", "filmmaking", "cinema"),
                publishedAt = "2026-07-15T09:00:00Z",
                readTimeMinutes = 8
            ),
            Article(
                id = "article-2",
                title = "Streaming Wars: What's Next for Movie Distribution",
                summary = "An analysis of how streaming platforms are reshaping the way we consume films.",
                content = "The battle for streaming supremacy continues to intensify as major studios launch their own platforms. With theatrical windows shrinking and simultaneous releases becoming more common, the traditional movie distribution model is undergoing a seismic shift. Industry analysts predict that hybrid release strategies will become the new norm, balancing theatrical exclusivity with streaming accessibility.",
                authorName = "Marcus Chen",
                authorAvatar = "https://api.example.com/avatars/marcus-chen.jpg",
                imageUrl = "https://api.example.com/images/streaming-wars.jpg",
                category = "Industry Analysis",
                tags = listOf("streaming", "distribution", "box office", "industry"),
                publishedAt = "2026-07-14T11:30:00Z",
                readTimeMinutes = 6
            ),
            Article(
                id = "article-3",
                title = "How AI is Transforming Film Post-Production",
                summary = "From color grading to sound design, artificial intelligence is revolutionizing post-production workflows.",
                content = "Artificial intelligence tools are rapidly becoming indispensable in film post-production. AI-powered color grading can now match scenes shot under different lighting conditions in seconds, while machine learning algorithms assist editors in sorting through hundreds of hours of footage. Sound designers are using AI to generate ambient soundscapes and clean up dialogue tracks with unprecedented precision.",
                authorName = "Sarah Kim",
                authorAvatar = "https://api.example.com/avatars/sarah-kim.jpg",
                imageUrl = "https://api.example.com/images/ai-post-production.jpg",
                category = "Technology",
                tags = listOf("AI", "post-production", "technology", "filmmaking"),
                publishedAt = "2026-07-13T14:00:00Z",
                readTimeMinutes = 10
            ),
            Article(
                id = "article-4",
                title = "The Art of Movie Soundtracks: A Deep Dive",
                summary = "Exploring how composers craft the perfect score to elevate storytelling on screen.",
                content = "A great soundtrack can transform a good film into an unforgettable experience. From Hans Zimmer's thundering orchestral pieces to Trent Reznor's electronic landscapes, modern film composers are pushing boundaries in how music enhances narrative. This article explores the creative process behind iconic scores, the collaboration between directors and composers, and the evolving role of music in cinema.",
                authorName = "David Park",
                authorAvatar = "https://api.example.com/avatars/david-park.jpg",
                imageUrl = "https://api.example.com/images/movie-soundtracks.jpg",
                category = "Film Craft",
                tags = listOf("soundtrack", "music", "composers", "film score"),
                publishedAt = "2026-07-12T08:45:00Z",
                readTimeMinutes = 12
            ),
            Article(
                id = "article-5",
                title = "International Cinema: Hidden Gems You Need to Watch",
                summary = "A curated selection of outstanding international films that deserve wider recognition.",
                content = "While Hollywood dominates the global box office, some of the most compelling storytelling comes from international cinema. From the poetic realism of Iranian films to the vibrant energy of South Korean thrillers, world cinema offers a treasure trove of experiences. This guide highlights ten recent international films that showcase the diversity and richness of global filmmaking.",
                authorName = "Amara Okafor",
                authorAvatar = "https://api.example.com/avatars/amara-okafor.jpg",
                imageUrl = "https://api.example.com/images/international-cinema.jpg",
                category = "Recommendations",
                tags = listOf("international", "world cinema", "recommendations", "hidden gems"),
                publishedAt = "2026-07-11T10:15:00Z",
                readTimeMinutes = 7
            ),
            Article(
                id = "article-6",
                title = "The Evolution of Movie Villains Through the Decades",
                summary = "From mustache-twirling caricatures to complex antiheroes: how movie villains have evolved.",
                content = "Movie villains have undergone a remarkable transformation over the decades. The one-dimensional antagonists of early cinema have given way to nuanced, psychologically complex characters who often steal the show. This article traces the evolution of cinematic villainy, examining how societal changes and audience expectations have shaped the way filmmakers portray the darker side of humanity.",
                authorName = "Thomas Reed",
                authorAvatar = "https://api.example.com/avatars/thomas-reed.jpg",
                imageUrl = "https://api.example.com/images/movie-villains.jpg",
                category = "Film History",
                tags = listOf("villains", "character study", "film history", "storytelling"),
                publishedAt = "2026-07-10T16:00:00Z",
                readTimeMinutes = 9
            ),
            Article(
                id = "article-7",
                title = "Behind the Scenes: The Unsung Heroes of Filmmaking",
                summary = "Spotlighting the essential crew members whose work often goes unnoticed by audiences.",
                content = "While actors and directors receive the lion's share of attention, films are brought to life by dozens of specialized crew members working behind the scenes. From the gaffer who sculpts light to the foley artist who creates sound effects by hand, these professionals are the backbone of cinema. This article celebrates the craft and dedication of the unsung heroes who make movie magic possible.",
                authorName = "Laura Bennett",
                authorAvatar = "https://api.example.com/avatars/laura-bennett.jpg",
                imageUrl = "https://api.example.com/images/behind-scenes.jpg",
                category = "Film Craft",
                tags = listOf("crew", "behind the scenes", "filmmaking", "production"),
                publishedAt = "2026-07-09T12:30:00Z",
                readTimeMinutes = 8
            ),
            Article(
                id = "article-8",
                title = "Movie Theaters in 2026: Reinventing the Cinema Experience",
                summary = "How theaters are adapting with premium formats, dining, and immersive technology.",
                content = "Facing competition from streaming services, movie theaters are reinventing themselves. From IMAX and Dolby Cinema to dine-in experiences and interactive screenings, exhibitors are betting on premium experiences that can't be replicated at home. This article explores the innovations driving the theatrical revival and what moviegoers can expect from the cinema of the future.",
                authorName = "Ryan Mitchell",
                authorAvatar = "https://api.example.com/avatars/ryan-mitchell.jpg",
                imageUrl = "https://api.example.com/images/cinema-2026.jpg",
                category = "Industry Analysis",
                tags = listOf("theaters", "cinema experience", "IMAX", "innovation"),
                publishedAt = "2026-07-08T09:00:00Z",
                readTimeMinutes = 6
            )
        )
    }

    override suspend fun getArticles(page: Int, pageSize: Int): ArticleListing {
        val allArticles = getMockArticles()
        val totalPages = ceil(allArticles.size.toDouble() / pageSize).toInt()
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, allArticles.size)

        val paginatedArticles = if (startIndex < allArticles.size) {
            allArticles.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return ArticleListing(
            totalPages = totalPages,
            totalResults = allArticles.size,
            page = page,
            articles = paginatedArticles
        )
    }

    override suspend fun getArticleById(articleId: String): Article? {
        return getMockArticles().find { it.id == articleId }
    }
}
