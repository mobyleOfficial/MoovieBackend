package com.moovie.backend.data.remote.comments;

import com.moovie.backend.domain.model.Comment;
import com.moovie.backend.domain.model.CommentListing;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class CommentsDataSourceImpl implements CommentsDataSource {

    private static final Map<String, List<Comment>> MOCK_COMMENTS = Map.of(
            "review-001", List.of(
                    new Comment("c1", "Alice", "https://i.pravatar.cc/150?u=alice", "Great review! Totally agree.", "2024-01-15T10:30:00Z", 4.5),
                    new Comment("c2", "Bob", "https://i.pravatar.cc/150?u=bob", "Interesting perspective.", "2024-01-15T11:00:00Z", 3.0),
                    new Comment("c3", "Carol", "https://i.pravatar.cc/150?u=carol", "I disagree, the movie was better than you described.", "2024-01-15T12:00:00Z", 2.0)
            ),
            "review-002", List.of(
                    new Comment("c4", "Dave", "https://i.pravatar.cc/150?u=dave", "Well written review!", "2024-01-16T09:00:00Z", 5.0),
                    new Comment("c5", "Eve", "https://i.pravatar.cc/150?u=eve", "Spot on analysis.", "2024-01-16T10:30:00Z", 4.0)
            ),
            "review-003", List.of(
                    new Comment("c6", "Frank", "https://i.pravatar.cc/150?u=frank", "Nice take on the cinematography.", "2024-01-17T14:00:00Z", 4.5)
            ),
            "review-004", List.of(
                    new Comment("c7", "Grace", "https://i.pravatar.cc/150?u=grace", "I loved this movie too!", "2024-01-18T08:00:00Z", 5.0),
                    new Comment("c8", "Hank", "https://i.pravatar.cc/150?u=hank", "The soundtrack was amazing.", "2024-01-18T09:30:00Z", 4.0),
                    new Comment("c9", "Ivy", "https://i.pravatar.cc/150?u=ivy", "Overrated in my opinion.", "2024-01-18T11:00:00Z", 2.5),
                    new Comment("c10", "Jack", "https://i.pravatar.cc/150?u=jack", "A must-watch for sure.", "2024-01-18T12:00:00Z", 4.5)
            ),
            "review-005", List.of(
                    new Comment("c11", "Karen", "https://i.pravatar.cc/150?u=karen", "Thanks for the recommendation!", "2024-01-19T07:00:00Z", 3.5)
            ),
            "list-001", List.of(
                    new Comment("c12", "Leo", "https://i.pravatar.cc/150?u=leo", "Great list! Adding these to my watchlist.", "2024-01-20T10:00:00Z", 5.0),
                    new Comment("c13", "Mia", "https://i.pravatar.cc/150?u=mia", "You missed a few classics though.", "2024-01-20T11:30:00Z", 3.5)
            ),
            "list-002", List.of(
                    new Comment("c14", "Nick", "https://i.pravatar.cc/150?u=nick", "Solid picks!", "2024-01-21T15:00:00Z", 4.0)
            )
    );

    @Override
    public CommentListing getComments(String contentId, int page, int pageSize) {
        List<Comment> allComments = MOCK_COMMENTS.getOrDefault(contentId, Collections.emptyList());
        int totalResults = allComments.size();
        int totalPages = (int) Math.ceil((double) totalResults / pageSize);

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalResults);

        List<Comment> pageComments = (fromIndex < totalResults)
                ? allComments.subList(fromIndex, toIndex)
                : Collections.emptyList();

        return new CommentListing(contentId, totalPages, totalResults, page, pageComments);
    }
}
