package com.resumepilot.service;

import com.resumepilot.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter (sliding window).
 *
 * <p>Used to protect OTP sending, OTP verification attempts and login attempts.
 * Single-node only; replace with Redis (e.g. via Spring Data Redis + SlidingWindow)
 * when scaling horizontally.</p>
 */
@Service
public class RateLimitService {

    /** Key -> (windowStart, count). */
    private final Map<String, Window> buckets = new ConcurrentHashMap<>();

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long cooldownSeconds;

    /** Enforces a fixed-window rate limit: max {@code max} hits per {@code window} duration. */
    public void enforce(String key, int max, Duration window) {
        Instant now = Instant.now();
        Window bucket = buckets.compute(key, (k, current) -> {
            if (current == null || current.startedAt().plus(window).isBefore(now)) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.count() + 1);
        });

        if (bucket.count() > max) {
            throw new RateLimitExceededException("Too many requests. Please try again later.");
        }
    }

    /** Cooldown check for OTP resend: one send per key within {@link #cooldownSeconds}. */
    public void enforceCooldown(String key) {
        enforce(key, 1, Duration.ofSeconds(cooldownSeconds));
    }

    private record Window(Instant startedAt, long count) {
    }
}
