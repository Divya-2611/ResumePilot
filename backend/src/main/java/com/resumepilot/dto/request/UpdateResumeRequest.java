package com.resumepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Updates an existing resume (content / structure / name / template).
 */
public record UpdateResumeRequest(

        @Size(max = 120, message = "Resume name must be at most 120 characters")
        String name,

        @Size(max = 40, message = "Invalid template")
        String template,

        String content,
        String structure,

        @jakarta.validation.constraints.Pattern(
                regexp = "^(true|false)$", message = "favorite must be true or false")
        String favorite
) {
}
