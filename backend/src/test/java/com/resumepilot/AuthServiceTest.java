package com.resumepilot;

import com.resumepilot.dto.request.ProfileUpdateRequest;
import com.resumepilot.dto.request.RegisterRequest;
import com.resumepilot.dto.response.AuthResponse;
import com.resumepilot.entity.OtpVerification.OtpType;
import com.resumepilot.entity.Role;
import com.resumepilot.entity.Role.RoleName;
import com.resumepilot.entity.User;
import com.resumepilot.exception.BadRequestException;
import com.resumepilot.exception.EmailAlreadyExistsException;
import com.resumepilot.exception.OtpException;
import com.resumepilot.mapper.UserMapper;
import com.resumepilot.repository.OtpVerificationRepository;
import com.resumepilot.repository.RefreshTokenRepository;
import com.resumepilot.repository.RoleRepository;
import com.resumepilot.repository.UserRepository;
import com.resumepilot.security.JwtService;
import com.resumepilot.service.AuthService;
import com.resumepilot.service.EmailService;
import com.resumepilot.service.OtpService;
import com.resumepilot.service.RateLimitService;
import com.resumepilot.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for registration + OTP verification flows.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private OtpVerificationRepository otpRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(
                "test-secret-key-with-enough-length-0123456789abcdef", 900_000, 604_800_000, 600_000);
        OtpService otpService = new OtpService(otpRepository, new RateLimitService());
        org.springframework.test.util.ReflectionTestUtils.setField(otpService, "otpLength", 6);
        org.springframework.test.util.ReflectionTestUtils.setField(otpService, "expirationMinutes", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(otpService, "maxPerEmailPerDay", 10);
        authService = new AuthService(
                userRepository, roleRepository, new BCryptPasswordEncoder(),
                authenticationManager, otpService,
                emailService, jwtService,
                new RefreshTokenService(refreshTokenRepository, jwtService),
                new RateLimitService());
    }

    private User verifiedUser() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("Password1!"));
        user.setEmailVerified(true);
        Role role = new Role(RoleName.USER);
        user.setRoles(Set.of(role));
        return user;
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "Password1!", "Different1!");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);
        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "Password1!", "Password1!");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void registerPersistsBcryptHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER))
                .thenReturn(Optional.of(new Role(RoleName.USER)));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest request = new RegisterRequest(
                "Jane", "Doe", "jane@example.com", "Password1!", "Password1!");
        authService.register(request);

        verify(userRepository).save(argThat(user ->
                new BCryptPasswordEncoder().matches("Password1!", user.getPassword())
                        && !user.getPassword().equals("Password1!")));
        verify(emailService).sendOtpEmail(eq("jane@example.com"), anyString(), anyString(), anyInt());
    }

    @Test
    void verifyEmailWithInvalidOtpThrows() {
        User user = verifiedUser();
        user.setEmailVerified(false);
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findByEmailAndTypeAndUsedFalseAndCode(
                "jane@example.com", OtpType.EMAIL_VERIFICATION, "123456"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("jane@example.com", "123456"))
                .isInstanceOf(OtpException.class);
        verify(userRepository, never()).save(user);
    }
}
