package com.moovie.backend.data.repository;

import com.moovie.backend.domain.model.*;
import com.moovie.backend.domain.repository.ProfileRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class ProfileRepositoryImpl implements ProfileRepository {

    private static final Map<String, PublicProfile> MOCK_PROFILES = Map.of(
            "user-001", new PublicProfile(
                    "user-001", "Alice", "AL", "Film enthusiast and aspiring critic.",
                    List.of(
                            new ProfileWatchedMovie(550, "Fight Club", "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg", 4.5),
                            new ProfileWatchedMovie(680, "Pulp Fiction", "/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg", 5.0)
                    ),
                    List.of(new ProfileUser("user-002", "Bob", "BO")),
                    List.of(new ProfileUser("user-003", "Carol", "CA")),
                    List.of(new ProfileFavoriteMovie(550, "Fight Club", "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg")),
                    List.of(new ProfileRecentActivity("reviewed", "Fight Club", "2024-01-15")),
                    List.of(new ProfileWatchlistItem(238, "The Godfather", "/3bhkrj58Vtu7enYsRolD1fZdja1.jpg"))
            ),
            "user-002", new PublicProfile(
                    "user-002", "Bob", "BO", "Casual movie watcher.",
                    List.of(new ProfileWatchedMovie(238, "The Godfather", "/3bhkrj58Vtu7enYsRolD1fZdja1.jpg", 4.0)),
                    Collections.emptyList(),
                    List.of(new ProfileUser("user-001", "Alice", "AL")),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            )
    );

    @Override
    public UserProfile getUserProfile() {
        return new UserProfile(
                null,
                "currentUser",
                "Movie lover",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @Override
    public void updateUserProfile(UserProfile profile) {
        // TODO: persist to database
    }

    @Override
    public PublicProfile getPublicProfile(String userId) {
        return MOCK_PROFILES.getOrDefault(userId, new PublicProfile(
                userId, "Unknown", "??", null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        ));
    }
}
