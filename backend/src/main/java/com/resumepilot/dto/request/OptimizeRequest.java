package com.resumepilot.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Triggers optimization of a resume against a job description.
 * The JD may be a pasted snippet (inline) or a previously stored JD id.
 */
public record OptimizeRequest(

        @NotNull(message = "Resume id is required")
        Long resumeId,

        /** Optional: reuse a stored JD. */
        Long jobDescriptionId,

        /** Optional: inline JD text (used when jobDescriptionId is absent). */
        String jobDescriptionText
) {
}
