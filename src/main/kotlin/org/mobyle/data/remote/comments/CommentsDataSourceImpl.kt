package org.mobyle.data.remote.comments

import org.mobyle.domain.model.Comment
import org.mobyle.domain.model.CommentListing
import kotlin.math.ceil

class CommentsDataSourceImpl : CommentsDataSource {

    companion object {
        private fun getMockComments(): Map<String, List<Comment>> = mapOf(
            "review-001" to listOf(
                Comment(
                    id = "comment-1",
                    authorName = "John Mitchell",
                    authorAvatar = "https://api.example.com/avatars/john-mitchell.jpg",
                    content = "This is an excellent and thoughtful review. Completely agree with your analysis of the character development.",
                    createdAt = "2026-05-10T14:30:00Z",
                    rating = 4.5
                ),
                Comment(
                    id = "comment-2",
                    authorName = "Sarah Thompson",
                    authorAvatar = "https://api.example.com/avatars/sarah-thompson.jpg",
                    content = "Great breakdown of the cinematography. The director really showcased the landscapes beautifully.",
                    createdAt = "2026-05-09T10:15:00Z",
                    rating = 4.0
                ),
                Comment(
                    id = "comment-3",
                    authorName = "Michael Chen",
                    authorAvatar = "https://api.example.com/avatars/michael-chen.jpg",
                    content = "I have to disagree with some of your points about the ending. Felt it was a bit rushed.",
                    createdAt = "2026-05-08T18:45:00Z",
                    rating = 3.0
                ),
                Comment(
                    id = "comment-4",
                    authorName = "Emma Wilson",
                    authorAvatar = "https://api.example.com/avatars/emma-wilson.jpg",
                    content = "Loved the soundtrack choices throughout. Really enhanced the emotional impact of key scenes.",
                    createdAt = "2026-05-07T11:20:00Z",
                    rating = 5.0
                ),
                Comment(
                    id = "comment-5",
                    authorName = "David Roberts",
                    authorAvatar = "https://api.example.com/avatars/david-roberts.jpg",
                    content = "The acting performances were outstanding. Each cast member brought depth to their roles.",
                    createdAt = "2026-05-06T09:00:00Z",
                    rating = 4.5
                )
            ),
            "review-002" to listOf(
                Comment(
                    id = "comment-6",
                    authorName = "Lisa Anderson",
                    authorAvatar = "https://api.example.com/avatars/lisa-anderson.jpg",
                    content = "Perfectly captured the essence of the original novel. A masterful adaptation.",
                    createdAt = "2026-05-10T16:30:00Z",
                    rating = 5.0
                ),
                Comment(
                    id = "comment-7",
                    authorName = "James Martinez",
                    authorAvatar = "https://api.example.com/avatars/james-martinez.jpg",
                    content = "The pacing felt off in the second act. Otherwise entertaining.",
                    createdAt = "2026-05-09T13:45:00Z",
                    rating = 3.5
                ),
                Comment(
                    id = "comment-8",
                    authorName = "Patricia Brown",
                    authorAvatar = "https://api.example.com/avatars/patricia-brown.jpg",
                    content = "Exceptional cinematography throughout. The visual storytelling was impeccable.",
                    createdAt = "2026-05-08T10:20:00Z",
                    rating = 4.5
                ),
                Comment(
                    id = "comment-9",
                    authorName = "Robert Taylor",
                    authorAvatar = "https://api.example.com/avatars/robert-taylor.jpg",
                    content = "Could not have asked for better character development. Truly compelling.",
                    createdAt = "2026-05-07T15:55:00Z",
                    rating = 5.0
                )
            ),
            "list-001" to listOf(
                Comment(
                    id = "comment-10",
                    authorName = "Maria Garcia",
                    authorAvatar = "https://api.example.com/avatars/maria-garcia.jpg",
                    content = "Excellent curated selection of films. This list captures the essence of the genre perfectly.",
                    createdAt = "2026-05-10T12:15:00Z",
                    rating = 4.5
                ),
                Comment(
                    id = "comment-11",
                    authorName = "Carlos Rodriguez",
                    authorAvatar = "https://api.example.com/avatars/carlos-rodriguez.jpg",
                    content = "Great list! I discovered some hidden gems here. Thank you for the recommendations.",
                    createdAt = "2026-05-09T09:30:00Z",
                    rating = 4.0
                ),
                Comment(
                    id = "comment-12",
                    authorName = "Anna Lopez",
                    authorAvatar = "https://api.example.com/avatars/anna-lopez.jpg",
                    content = "Thought-provoking collection. Really enjoyed the diversity of storytelling approaches.",
                    createdAt = "2026-05-08T14:20:00Z",
                    rating = 4.5
                ),
                Comment(
                    id = "comment-13",
                    authorName = "Miguel Sanchez",
                    authorAvatar = "https://api.example.com/avatars/miguel-sanchez.jpg",
                    content = "Missing a few classics from this genre. Overall, solid recommendations.",
                    createdAt = "2026-05-07T08:45:00Z",
                    rating = 3.5
                ),
                Comment(
                    id = "comment-14",
                    authorName = "Sofia Hernandez",
                    authorAvatar = "https://api.example.com/avatars/sofia-hernandez.jpg",
                    content = "Absolutely loved this compilation. Every film is a masterpiece.",
                    createdAt = "2026-05-06T17:10:00Z",
                    rating = 5.0
                )
            ),
            "review-003" to listOf(
                Comment(
                    id = "comment-15",
                    authorName = "Thomas Williams",
                    authorAvatar = "https://api.example.com/avatars/thomas-williams.jpg",
                    content = "Insightful analysis of the film's themes. Great perspective on the symbolism.",
                    createdAt = "2026-05-10T11:00:00Z",
                    rating = 4.0
                ),
                Comment(
                    id = "comment-16",
                    authorName = "Jennifer Davis",
                    authorAvatar = "https://api.example.com/avatars/jennifer-davis.jpg",
                    content = "Disagree with your interpretation of the ending, but well-written review nonetheless.",
                    createdAt = "2026-05-09T16:25:00Z",
                    rating = 3.5
                ),
                Comment(
                    id = "comment-17",
                    authorName = "Christopher Jones",
                    authorAvatar = "https://api.example.com/avatars/christopher-jones.jpg",
                    content = "Excellent breakdown of the director's technique. Very informative.",
                    createdAt = "2026-05-08T13:50:00Z",
                    rating = 4.5
                )
            ),
            "review-004" to listOf(
                Comment(
                    id = "comment-18",
                    authorName = "Victoria Moore",
                    authorAvatar = "https://api.example.com/avatars/victoria-moore.jpg",
                    content = "One of the most balanced reviews I've read about this film.",
                    createdAt = "2026-05-10T10:30:00Z",
                    rating = 4.5
                ),
                Comment(
                    id = "comment-19",
                    authorName = "Andrew Jackson",
                    authorAvatar = "https://api.example.com/avatars/andrew-jackson.jpg",
                    content = "Your points about the soundtrack are spot on. Great attention to detail.",
                    createdAt = "2026-05-09T14:00:00Z",
                    rating = 4.0
                ),
                Comment(
                    id = "comment-20",
                    authorName = "Margaret White",
                    authorAvatar = "https://api.example.com/avatars/margaret-white.jpg",
                    content = "Thoroughly enjoyed your review. It gave me new appreciation for the film.",
                    createdAt = "2026-05-08T09:15:00Z",
                    rating = 4.5
                )
            ),
            "list-002" to listOf(
                Comment(
                    id = "comment-21",
                    authorName = "Kevin Anderson",
                    authorAvatar = "https://api.example.com/avatars/kevin-anderson.jpg",
                    content = "Perfect list for movie night recommendations. Bookmarking this!",
                    createdAt = "2026-05-10T15:45:00Z",
                    rating = 4.5
                ),
                Comment(
                    id = "comment-22",
                    authorName = "Rachel Green",
                    authorAvatar = "https://api.example.com/avatars/rachel-green.jpg",
                    content = "Love how you organized this. Makes it easy to find something to watch.",
                    createdAt = "2026-05-09T12:20:00Z",
                    rating = 4.0
                ),
                Comment(
                    id = "comment-23",
                    authorName = "Daniel Robinson",
                    authorAvatar = "https://api.example.com/avatars/daniel-robinson.jpg",
                    content = "Some interesting picks here. Already watched most of these, but great collection.",
                    createdAt = "2026-05-08T11:35:00Z",
                    rating = 4.0
                )
            ),
            "review-005" to listOf(
                Comment(
                    id = "comment-24",
                    authorName = "Jessica Lee",
                    authorAvatar = "https://api.example.com/avatars/jessica-lee.jpg",
                    content = "Best review of this film I've encountered. Very thorough analysis.",
                    createdAt = "2026-05-10T13:20:00Z",
                    rating = 5.0
                ),
                Comment(
                    id = "comment-25",
                    authorName = "Matthew White",
                    authorAvatar = "https://api.example.com/avatars/matthew-white.jpg",
                    content = "While I enjoyed the film, I think you might be a bit too generous with the rating.",
                    createdAt = "2026-05-09T11:50:00Z",
                    rating = 3.5
                ),
                Comment(
                    id = "comment-26",
                    authorName = "Lisa Johnson",
                    authorAvatar = "https://api.example.com/avatars/lisa-johnson.jpg",
                    content = "Your passion for this film really comes through in your writing. Inspiring.",
                    createdAt = "2026-05-08T10:05:00Z",
                    rating = 4.5
                )
            )
        )
    }

    override suspend fun getComments(contentId: String, page: Int, pageSize: Int): CommentListing {
        val allComments = getMockComments()
        val contentComments = allComments[contentId] ?: emptyList()

        val totalPages = ceil(contentComments.size.toDouble() / pageSize).toInt()
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, contentComments.size)

        val paginatedComments = if (startIndex < contentComments.size) {
            contentComments.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return CommentListing(
            contentId = contentId,
            totalPages = totalPages,
            totalResults = contentComments.size,
            page = page,
            comments = paginatedComments
        )
    }
}
