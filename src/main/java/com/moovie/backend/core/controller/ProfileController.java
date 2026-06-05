package com.moovie.backend.core.controller;

import com.moovie.backend.domain.model.PublicProfile;
import com.moovie.backend.domain.model.UserProfile;
import com.moovie.backend.domain.usecase.profile.GetPublicProfile;
import com.moovie.backend.domain.usecase.profile.GetUserProfile;
import com.moovie.backend.domain.usecase.profile.UpdateUserProfile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final GetUserProfile getUserProfile;
    private final UpdateUserProfile updateUserProfile;
    private final GetPublicProfile getPublicProfile;

    public ProfileController(GetUserProfile getUserProfile,
                             UpdateUserProfile updateUserProfile,
                             GetPublicProfile getPublicProfile) {
        this.getUserProfile = getUserProfile;
        this.updateUserProfile = updateUserProfile;
        this.getPublicProfile = getPublicProfile;
    }

    @GetMapping
    public UserProfile profile() {
        return getUserProfile.invoke();
    }

    @PutMapping
    public void updateProfile(@RequestBody UserProfile profile) {
        updateUserProfile.invoke(profile);
    }

    @GetMapping("/{userId}")
    public PublicProfile publicProfile(@PathVariable String userId) {
        return getPublicProfile.invoke(userId);
    }
}
