package com.resumepilot.repository;

import com.resumepilot.entity.OptimizationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OptimizationHistoryRepository extends JpaRepository<OptimizationHistory, Long> {

    @Query("""
            select h from OptimizationHistory h
            left join h.resume r
            left join h.jobDescription jd
            where h.user.id = :userId
            and (:query is null
                 or lower(r.name) like :pattern
                 or lower(coalesce(jd.title, '')) like :pattern)
            order by h.createdAt desc
            """)
    Page<OptimizationHistory> search(@Param("userId") Long userId,
                                     @Param("query") String query,
                                     @Param("pattern") String pattern,
                                     Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, java.time.LocalDateTime after);

    List<OptimizationHistory> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    List<OptimizationHistory> findByUserId(Long userId);

    @Modifying
    @Query("delete from OptimizationHistory h where h.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from OptimizationHistory h where h.resume.id = :resumeId")
    void deleteByResumeId(@Param("resumeId") Long resumeId);

    @Modifying
    @Query("update OptimizationHistory h set h.resultVersion = null where h.resultVersion.id = :versionId")
    void clearResultVersion(@Param("versionId") Long versionId);
}
