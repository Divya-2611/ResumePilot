package com.resumepilot.repository;

import com.resumepilot.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {

    List<ResumeVersion> findByResumeIdOrderByCreatedAtDesc(Long resumeId);

    Optional<ResumeVersion> findByIdAndResumeId(Long id, Long resumeId);

    Optional<ResumeVersion> findByIdAndResumeUserId(Long id, Long userId);

    long countByResumeId(Long resumeId);
}
