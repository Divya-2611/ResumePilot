package com.resumepilot.mapper;

import com.resumepilot.dto.response.UserResponse;
import com.resumepilot.entity.User;
import com.resumepilot.util.DateTimeUtil;

import java.util.List;

/**
 * Maps between User entity and DTOs.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList();
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isEmailVerified() ? "VERIFIED" : "PENDING",
                roles,
                DateTimeUtil.toIsoString(user.getCreatedAt()));
    }
}
