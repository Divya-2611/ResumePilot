package com.resumepilot.service;

import com.resumepilot.dto.request.*;
import com.resumepilot.dto.response.AuthResponse;
import com.resumepilot.dto.response.UserResponse;
import com.resumepilot.entity.OtpVerification.OtpType;
import com.resumepilot.entity.RefreshToken;
import com.resumepilot.entity.Role;
import com.resumepilot.entity.Role.RoleName;
import com.resumepilot.entity.User;
import com.resumepilot.exception.BadRequestException;
import com.resumepilot.exception.EmailAlreadyExistsException;
import com.resumepilot.exception.ResourceNotFoundException;
import com.resumepilot.exception.UnauthorizedException;
import com.resumepilot.mapper.UserMapper;
import com.resumepilot.repository.RoleRepository;
import com.resumepilot.repository.UserRepository;
import com.resumepilot.security.JwtService;
import com.resumepilot.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;

/**
 * Orchestrates the complete authentication flow:
 * register -> OTP verify -> login/refresh/logout, plus password reset & change.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitService rateLimitService;

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    /** Creates an unverified account and emails a 6-digit OTP. */
    @Transactional
    public void register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", RoleName.USER));
        user.setRoles(Set.of(userRole));
        userRepository.save(user);

        sendOtp(user.getEmail(), OtpType.EMAIL_VERIFICATION, "Your verification code");
        log.info("Registered user {}", user.getEmail());
    }

    /** Verifies the signup OTP, marks the account verified and issues tokens. */
    @Transactional
    public AuthResponse verifyEmail(String email, String otp) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ResourceNotFoundException.of("User", email));
        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified. Please log in.");
        }

        rateLimitService.enforce("otp:verify:" + email.toLowerCase(), 5, Duration.ofMinutes(15));
        otpService.verifyOtp(email, OtpType.EMAIL_VERIFICATION, otp);

        user.setEmailVerified(true);
        userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
        return issueTokens(user);
    }

    /** Re-sends the signup OTP (rate limited). */
    public void resendVerificationOtp(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ResourceNotFoundException.of("User", email));
        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        sendOtp(email, OtpType.EMAIL_VERIFICATION, "Your new verification code");
    }

    // ------------------------------------------------------------------
    // Login / Refresh / Logout
    // ------------------------------------------------------------------

    /** Authenticates credentials and issues an access + refresh token pair. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        rateLimitService.enforce("login:" + request.email().toLowerCase(), 5, Duration.ofMinutes(15));

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().trim().toLowerCase(), request.password()));
            User user = ((SecurityUser) authentication.getPrincipal()).getUser();
            userRepository.updateLastLogin(user.getId());
            return issueTokens(user);
        } catch (LockedException e) {
            throw new UnauthorizedException("Account is locked. Contact support.");
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    /** Rotates the refresh token and returns a new pair. */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedToken rotated =
                refreshTokenService.rotate(rawRefreshToken);
        String email = jwtService.extractSubject(rotated.accessToken());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        return AuthResponse.of(rotated.accessToken(), rotated.refreshToken(),
                jwtService.getAccessExpirationMs() / 1000, UserMapper.toResponse(user));
    }

    /** Logs out by revoking the presented refresh token. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    // ------------------------------------------------------------------
    // Password flows
    // ------------------------------------------------------------------

    /** Sends a password-reset OTP (only when the email exists - no enumeration). */
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user ->
                sendOtp(user.getEmail(), OtpType.PASSWORD_RESET, "Password reset code"));
    }

    /** Verifies the reset OTP and returns a short-lived reset JWT. */
    public String verifyResetOtp(String email, String otp) {
        otpService.verifyOtp(email, OtpType.PASSWORD_RESET, otp);
        return jwtService.generateResetToken(email.toLowerCase());
    }

    /** Applies a new password, revokes all refresh tokens and notifies the user. */
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        String email = jwtService.extractSubject(resetToken);
        if (!"RESET".equals(jwtService.extractType(resetToken))) {
            throw new UnauthorizedException("Invalid reset token");
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
        otpService.invalidateAll(email, OtpType.PASSWORD_RESET);
        emailService.sendPasswordResetConfirmation(email);
    }

    /** Changes password for an authenticated user. */
    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
        emailService.sendPasswordResetConfirmation(user.getEmail());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Sends an OTP and logs the code (dev aid) when mail sending is unavailable. */
    private void sendOtp(String email, OtpType type, String subject) {
        String code = otpService.generateOtp(email, type);
        emailService.sendOtpEmail(email, subject, code, 5);
        log.info("OTP for {} ({}): {}", email, type, code);
    }

    private AuthResponse issueTokens(User user) {
        SecurityUser securityUser = new SecurityUser(user);
        String accessToken = jwtService.generateAccessToken(securityUser);
        String refreshToken = jwtService.generateRefreshToken(securityUser);
        refreshTokenService.store(refreshToken, user);
        return AuthResponse.of(accessToken, refreshToken,
                jwtService.getAccessExpirationMs() / 1000, UserMapper.toResponse(user));
    }
}
