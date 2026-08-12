package com.resumepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Saves an optimization result as a new version of a resume.
 */
public record SaveVersionRequest(

        @NotNull(message = "Resume id is required")
        Long resumeId,

        @NotBlank(message = "Version name is required")
        @Size(max = 120, message = "Version name must be at most 120 characters")
        String name,

        @Size(max = 255, message = "Label must be at most 255 characters")
        String label,

        String content,
        String structure,
        Integer atsScore,
        Integer keywordMatchPercent
) {
}
