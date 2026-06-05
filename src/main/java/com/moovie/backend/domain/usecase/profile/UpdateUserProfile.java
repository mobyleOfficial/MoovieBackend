package com.moovie.backend.domain.usecase.profile;

import com.moovie.backend.domain.model.UserProfile;
import com.moovie.backend.domain.repository.ProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateUserProfile {

    private final ProfileRepository profileRepository;

    public UpdateUserProfile(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public void invoke(UserProfile profile) {
        profileRepository.updateUserProfile(profile);
    }
}
