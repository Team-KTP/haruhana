package org.kwakmunsu.haruhana.domain.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    boolean existsByLoginIdAndStatus(String loginId, EntityStatus status);
    boolean existsByNicknameAndStatus(String nickname, EntityStatus status);
    Optional<Member> findByLoginIdAndStatus(String loginId, EntityStatus status);
    Optional<Member> findByIdAndStatus(Long id, EntityStatus entityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.status = :status")
    Optional<Member> findByIdAndStatusWithLock(@Param("id") Long id, @Param("status") EntityStatus status);
    Optional<Member> findByRefreshTokenAndStatus(String refreshToken, EntityStatus status);

    @Query("""
        SELECT m FROM Member m
        WHERE m.status = :status
        AND m.role = :role
        AND NOT EXISTS (
            SELECT 1 FROM Submission s
            WHERE s.member = m
            AND s.status = :status
            AND s.submittedAt >= :startOfDay
            AND s.submittedAt < :endOfDay
        )
        """)
    List<Member> findMembersWithoutTodaySubmission(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("role") Role role,
            @Param("status") EntityStatus status
    );

    long countAllByStatus(EntityStatus status);

}