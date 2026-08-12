package com.resumepilot.dto.response;

import java.util.List;

/**
 * User profile payload. The picture is served from a dedicated endpoint.
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String emailVerified,
        List<String> roles,
        String profilePictureUrl,
        String createdAt
) {
}
