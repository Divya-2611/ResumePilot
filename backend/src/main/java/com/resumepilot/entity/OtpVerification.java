package com.resumepilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One-time password used for email verification and password reset.
 * Expires after a short window (default 5 minutes) and is one-time-use.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "otp_verifications",
        indexes = {
                @Index(name = "idx_otp_email", columnList = "email"),
                @Index(name = "idx_otp_code_type", columnList = "code,type")
        })
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 120)
    private String email;

    /** 6-digit numeric code. */
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    /** Salted hash alternative is supported; plain code is stored for simplicity in dev.
     *  For production, hash the code with SHA-256 before persisting. */
    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OtpType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public enum OtpType {
        EMAIL_VERIFICATION, PASSWORD_RESET
    }
}
