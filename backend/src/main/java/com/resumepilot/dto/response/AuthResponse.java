package com.resumepilot.dto.response;

import java.util.List;

/**
 * JWT auth result returned on login, OTP verification and token refresh.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {

    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
