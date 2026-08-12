package com.resumepilot.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token rotation payload.
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
