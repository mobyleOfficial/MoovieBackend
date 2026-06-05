package com.moovie.backend.domain.usecase.profile;

import com.moovie.backend.domain.model.PublicProfile;
import com.moovie.backend.domain.repository.ProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class GetPublicProfile {

    private final ProfileRepository profileRepository;

    public GetPublicProfile(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public PublicProfile invoke(String userId) {
        return profileRepository.getPublicProfile(userId);
    }
}
