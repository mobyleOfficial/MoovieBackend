package com.moovie.backend.domain.usecase.profile;

import com.moovie.backend.domain.model.UserProfile;
import com.moovie.backend.domain.repository.ProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class GetUserProfile {

    private final ProfileRepository profileRepository;

    public GetUserProfile(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public UserProfile invoke() {
        return profileRepository.getUserProfile();
    }
}
