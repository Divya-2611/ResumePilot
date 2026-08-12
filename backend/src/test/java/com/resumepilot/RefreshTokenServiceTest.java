package com.resumepilot;

import com.resumepilot.service.RefreshTokenService;
import com.resumepilot.entity.RefreshToken;
import com.resumepilot.entity.User;
import com.resumepilot.exception.UnauthorizedException;
import com.resumepilot.repository.RefreshTokenRepository;
import com.resumepilot.security.JwtService;
import com.resumepilot.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies refresh token rotation: old token revoked, new token issued.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(
                "test-secret-key-with-enough-length-0123456789abcdef",
                900_000, 604_800_000, 600_000);
        service = new RefreshTokenService(refreshTokenRepository, jwtService);

        user = new User();
        user.setId(1L);
        user.setEmail("jane@example.com");
    }

    @Test
    void rotateRevokesOldTokenAndIssuesNewPair() {
        String oldToken = new JwtService(
                "test-secret-key-with-enough-length-0123456789abcdef",
                900_000, 604_800_000, 600_000)
                .generateRefreshToken(new SecurityUser(user));

        RefreshToken stored = new RefreshToken();
        stored.setId(1L);
        stored.setUser(user);
        stored.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any()))
                .thenReturn(Optional.of(stored));

        RefreshTokenService.RotatedToken rotated = service.rotate(oldToken);

        assertThat(rotated.accessToken()).isNotBlank();
        assertThat(rotated.refreshToken()).isNotBlank();
        assertThat(rotated.refreshToken()).isNotEqualTo(oldToken);
        assertThat(stored.isRevoked()).isTrue();

        // Old token revoked (save 1) + new token persisted (save 2).
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).hasSize(2);
    }

    @Test
    void unknownTokenIsRejected() {
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("bogus-token"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
