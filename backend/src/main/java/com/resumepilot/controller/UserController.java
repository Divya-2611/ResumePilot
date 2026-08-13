package com.resumepilot.controller;

import com.resumepilot.dto.request.ChangePasswordRequest;
import com.resumepilot.dto.request.ProfileUpdateRequest;
import com.resumepilot.dto.response.ApiResponse;
import com.resumepilot.dto.response.UserResponse;
import com.resumepilot.service.AuthService;
import com.resumepilot.service.UserService;
import com.resumepilot.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authenticated profile endpoints.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(SecurityUtils.currentUserId())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                userService.updateProfile(SecurityUtils.currentUserId(), request)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.currentUser(), request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password changed. You have been logged out on all devices."));
    }

    @DeleteMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        Long userId = SecurityUtils.currentUserId();
        userService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Account deleted"));
    }
}
