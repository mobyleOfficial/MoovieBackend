package com.moovie.backend.domain.repository;

import com.moovie.backend.domain.model.PublicProfile;
import com.moovie.backend.domain.model.UserProfile;

public interface ProfileRepository {

    UserProfile getUserProfile();

    void updateUserProfile(UserProfile profile);

    PublicProfile getPublicProfile(String userId);
}
