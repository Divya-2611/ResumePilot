package com.resumepilot.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Resets the password using a short-lived JWT issued after OTP verification.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "Reset token is required")
        String resetToken,

        @NotBlank(message = "New password is required")
        @jakarta.validation.constraints.Size(min = 8, max = 64,
                message = "Password must be between 8 and 64 characters")
        String newPassword
) {
}
