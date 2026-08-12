package com.resumepilot.repository;

import com.resumepilot.entity.OtpVerification;
import com.resumepilot.entity.OtpVerification.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    List<OtpVerification> findByEmailAndTypeOrderByCreatedAtDesc(String email, OtpType type);

    Optional<OtpVerification> findByEmailAndTypeAndUsedFalseAndCode(
            String email, OtpType type, String code);
}
