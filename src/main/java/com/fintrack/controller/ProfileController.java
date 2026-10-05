package com.fintrack.controller;

import com.fintrack.dto.UserResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse getProfile(
            @AuthenticationPrincipal
            CustomUserPrincipal user
    ) {

        return userService.getUserById(
                user.getUserId()
        );
    }
}