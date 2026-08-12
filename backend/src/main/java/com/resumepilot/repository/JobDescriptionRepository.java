package com.resumepilot.repository;

import com.resumepilot.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {

    Optional<JobDescription> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);
}
