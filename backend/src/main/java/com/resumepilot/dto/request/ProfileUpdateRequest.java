package com.resumepilot.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Profile update payload (partial update allowed).
 */
public record ProfileUpdateRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 60, message = "First name must be at most 60 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 60, message = "Last name must be at most 60 characters")
        String lastName,

        @Email(message = "Invalid email format")
        @Size(max = 120, message = "Email must be at most 120 characters")
        String email
) {
}
