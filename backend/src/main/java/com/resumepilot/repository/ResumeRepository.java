package com.resumepilot.repository;

import com.resumepilot.entity.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    @Query("""
            select r from Resume r where r.user.id = :userId
            and (:query is null or lower(r.name) like :pattern)
            and (:favoritesOnly = false or r.favorite = true)
            order by r.updatedAt desc
            """)
    Page<Resume> search(@Param("userId") Long userId,
                        @Param("query") String query,
                        @Param("pattern") String pattern,
                        @Param("favoritesOnly") boolean favoritesOnly,
                        Pageable pageable);

    @Query("select r from Resume r where r.user.id = :userId order by r.updatedAt desc limit :limit")
    List<Resume> findRecent(@Param("userId") Long userId, @Param("limit") int limit);

    Optional<Resume> findByIdAndUserId(Long id, Long userId);

    List<Resume> findByUserId(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndFavoriteTrue(Long userId);
}
