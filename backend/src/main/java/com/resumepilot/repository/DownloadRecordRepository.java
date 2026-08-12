package com.resumepilot.repository;

import com.resumepilot.entity.DownloadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DownloadRecordRepository extends JpaRepository<DownloadRecord, Long> {

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByResumeId(Long resumeId);

    @Modifying
    @Query("update DownloadRecord d set d.version = null where d.version.id = :versionId")
    void clearVersion(@Param("versionId") Long versionId);
}
