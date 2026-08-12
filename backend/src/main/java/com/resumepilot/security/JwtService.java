package com.resumepilot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and validates JWT tokens (access, refresh and password-reset).
 *
 * <p>Token lifetimes are driven by configuration:
 * <ul>
 *   <li>Access token: 15 minutes</li>
 *   <li>Refresh token: 7 days (rotated on every use)</li>
 *   <li>Reset token: 10 minutes</li>
 * </ul></p>
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;
    private final long resetExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshExpirationMs,
            @Value("${app.jwt.reset-token-expiration-ms}") long resetExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.resetExpirationMs = resetExpirationMs;
    }

    /** Issues an access token for the given user. */
    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails, accessExpirationMs, "ACCESS");
    }

    /** Issues a refresh token for the given user. */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails, refreshExpirationMs, "REFRESH");
    }

    /** Issues a short-lived token authorizing a password reset. */
    public String generateResetToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("type", "RESET")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + resetExpirationMs))
                .signWith(key)
                .compact();
    }

    private String generateToken(UserDetails userDetails, long expirationMs, String type) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("type", type)
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .toList())
                // Unique token id guarantees rotation always produces a fresh token.
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Extracts the subject (email) from a token. */
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extracts the token type claim. */
    public String extractType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    /** Validates that the token is signed correctly, unexpired and matches the expected type. */
    public boolean isValid(String token, UserDetails userDetails, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equals(userDetails.getUsername())
                    && claims.getExpiration().after(new Date())
                    && expectedType.equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
