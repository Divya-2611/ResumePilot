package com.resumepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creates a blank resume (optionally from a template).
 */
public record CreateResumeRequest(

        @NotBlank(message = "Resume name is required")
        @Size(max = 120, message = "Resume name must be at most 120 characters")
        String name,

        @Size(max = 40, message = "Invalid template")
        String template,

        String content,
        String structure
) {
}
