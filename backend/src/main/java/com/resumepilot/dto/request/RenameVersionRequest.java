package com.resumepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Renames a version snapshot.
 */
public record RenameVersionRequest(

        @NotBlank(message = "Version name is required")
        @Size(max = 120, message = "Version name must be at most 120 characters")
        String name
) {
}