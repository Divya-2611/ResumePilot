package com.resumepilot.service;

import com.resumepilot.entity.OtpVerification;
import com.resumepilot.entity.OtpVerification.OtpType;
import com.resumepilot.exception.OtpException;
import com.resumepilot.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Issues and verifies one-time passwords (email verification + password reset).
 *
 * <p>Rules:
 * <ul>
 *   <li>6-digit numeric code, expires after 5 minutes.</li>
 *   <li>Rate limited: resend cooldown 60s, max 10 sends per email per day.</li>
 *   <li>Max 5 verification attempts per OTP, then the code is invalidated.</li>
 *   <li>One-time use only.</li>
 * </ul></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final OtpVerificationRepository otpRepository;
    private final RateLimitService rateLimitService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiration-minutes:5}")
    private int expirationMinutes;

    @Value("${app.otp.max-per-email-per-day:10}")
    private int maxPerEmailPerDay;

    /**
     * Generates, persists and returns a fresh OTP for the given email/type.
     * Rate limited by email+type (daily cap) and by resend cooldown.
     */
    @Transactional
    public String generateOtp(String email, OtpType type) {
        String dailyKey = "otp:daily:" + type + ":" + email.toLowerCase();
        rateLimitService.enforce(dailyKey, maxPerEmailPerDay, Duration.ofDays(1));

        String cooldownKey = "otp:cooldown:" + type + ":" + email.toLowerCase();
        rateLimitService.enforceCooldown(cooldownKey);

        // Invalidate any previous unused codes for this email+type.
        otpRepository.findByEmailAndTypeOrderByCreatedAtDesc(email, type)
                .forEach(o -> o.setUsed(true));

        String code = generateRandomCode();

        OtpVerification otp = new OtpVerification();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setType(type);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        otpRepository.save(otp);

        log.info("Generated {} OTP for {}", type, email);
        return code;
    }

    /**
     * Verifies a submitted code. Throws on invalid/expired codes.
     * Invalid attempts increment the counter; too many attempts burn the code.
     */
    @Transactional
    public void verifyOtp(String email, OtpType type, String code) {
        OtpVerification otp = otpRepository
                .findByEmailAndTypeAndUsedFalseAndCode(email, type, code)
                .orElseThrow(() -> new OtpException("Invalid OTP. Please check the code and try again."));

        if (otp.isExpired()) {
            otp.setUsed(true);
            otpRepository.save(otp);
            throw new OtpException("OTP has expired. Please request a new one.");
        }

        if (otp.getAttempts() >= MAX_VERIFY_ATTEMPTS) {
            otp.setUsed(true);
            otpRepository.save(otp);
            throw new OtpException("Too many invalid attempts. Please request a new OTP.");
        }

        otp.setUsed(true);
        otpRepository.save(otp);
    }

    /** Marks all pending codes for an email as used (e.g. after password reset). */
    @Transactional
    public void invalidateAll(String email, OtpType type) {
        otpRepository.findByEmailAndTypeOrderByCreatedAtDesc(email, type)
                .forEach(o -> o.setUsed(true));
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(otpLength);
        for (int i = 0; i < otpLength; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}
