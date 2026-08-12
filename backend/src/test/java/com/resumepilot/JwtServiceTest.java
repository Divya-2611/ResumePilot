package com.resumepilot;

import com.resumepilot.security.JwtService;
import com.resumepilot.security.SecurityUser;
import com.resumepilot.entity.Role;
import com.resumepilot.entity.Role.RoleName;
import com.resumepilot.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies JWT generation, validation and token-type gating.
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // 64-char secret (HS256 requires >= 32 bytes).
        String secret = "unit-test-secret-key-with-enough-length-0123456789abcdef";
        jwtService = new JwtService(secret, 900_000, 604_800_000, 600_000);
    }

    private UserDetails user() {
        User user = new User();
        user.setId(1L);
        user.setEmail("jane@example.com");
        user.setPassword("$2a$12$hash");
        Role role = new Role(RoleName.USER);
        user.setRoles(Set.of(role));
        return new SecurityUser(user);
    }

    @Test
    void accessTokenIsValidForUser() {
        UserDetails details = user();
        String token = jwtService.generateAccessToken(details);

        assertThat(jwtService.extractSubject(token)).isEqualTo("jane@example.com");
        assertThat(jwtService.isValid(token, details, "ACCESS")).isTrue();
    }

    @Test
    void refreshTokenIsRejectedByAccessCheck() {
        UserDetails details = user();
        String token = jwtService.generateRefreshToken(details);

        // Type gating: refresh token must not authenticate as an access token.
        assertThat(jwtService.isValid(token, details, "ACCESS")).isFalse();
        assertThat(jwtService.isValid(token, details, "REFRESH")).isTrue();
    }

    @Test
    void expiredTokenIsInvalid() {
        UserDetails details = user();
        String token = jwtService.generateAccessToken(details);

        // Simulate expiry by constructing a token in the past via a short-lived service.
        JwtService shortLived = new JwtService(
                "unit-test-secret-key-with-enough-length-0123456789abcdef",
                -1000, 1000, 1000);
        String expired = shortLived.generateAccessToken(details);

        assertThat(jwtService.isValid(expired, details, "ACCESS")).isFalse();
        assertThat(jwtService.isValid(token, details, "ACCESS")).isTrue();
    }

    @Test
    void resetTokenCarriesResetType() {
        String token = jwtService.generateResetToken("bob@example.com");
        assertThat(jwtService.extractSubject(token)).isEqualTo("bob@example.com");
        assertThat(jwtService.extractType(token)).isEqualTo("RESET");
    }
}
