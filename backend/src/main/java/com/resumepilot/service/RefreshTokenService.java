package com.resumepilot.service;

import com.resumepilot.entity.RefreshToken;
import com.resumepilot.entity.User;
import com.resumepilot.exception.UnauthorizedException;
import com.resumepilot.repository.RefreshTokenRepository;
import com.resumepilot.security.JwtService;
import com.resumepilot.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Persists refresh tokens (SHA-256 hashed) and implements rotation:
 * every /refresh call revokes the presented token and issues a new pair,
 * so a stolen token is only usable once.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    /** Stores a newly issued refresh token for the user. */
    @Transactional
    public void store(String rawToken, User user) {
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(LocalDateTime.now()
                .plusSeconds(jwtService.getRefreshExpirationMs() / 1000));
        entity.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(entity);
    }

    /**
     * Rotates the presented refresh token:
     * validates it, revokes it, issues + persists a new one and returns the new pair.
     */
    @Transactional
    public RotatedToken rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid or revoked refresh token"));

        if (stored.isExpired()) {
            stored.revoke();
            refreshTokenRepository.save(stored);
            throw new UnauthorizedException("Refresh token expired. Please log in again.");
        }

        User user = stored.getUser();
        SecurityUser securityUser = new SecurityUser(user);

        String newRefreshToken = jwtService.generateRefreshToken(securityUser);
        String newAccessToken = jwtService.generateAccessToken(securityUser);

        // Rotation: revoke old, persist new.
        stored.revoke();
        refreshTokenRepository.save(stored);
        store(newRefreshToken, user);

        return new RotatedToken(newAccessToken, newRefreshToken);
    }

    /** Revokes a single token (logout). */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(rawToken))
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }

    /** Revokes every active token for a user (password change / account deletion). */
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    /** Housekeeping: purge expired tokens hourly. */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void purgeExpired() {
        refreshTokenRepository.deleteExpired(LocalDateTime.now());
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record RotatedToken(String accessToken, String refreshToken) {
    }
}
