package com.resumepilot.dto.response;

import java.util.List;

/**
 * User profile payload.
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String emailVerified,
        List<String> roles,
        String createdAt
) {
}
