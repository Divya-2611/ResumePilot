package com.resumepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pastes a job description for analysis.
 */
public record PasteJdRequest(

        @NotBlank(message = "Job description content is required")
        @Size(max = 20000, message = "Job description too long (max 20,000 characters)")
        String content,

        @Size(max = 160, message = "Title must be at most 160 characters")
        String title
) {
}
