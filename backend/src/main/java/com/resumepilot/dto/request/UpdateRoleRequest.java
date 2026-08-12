package com.resumepilot.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin: promotes/demotes a user by role name.
 */
public record UpdateRoleRequest(@NotBlank String role) {
}