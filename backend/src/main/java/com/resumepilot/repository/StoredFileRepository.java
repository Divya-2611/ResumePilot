package com.resumepilot.repository;

import com.resumepilot.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    void deleteByUserId(Long userId);
}
