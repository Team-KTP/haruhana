package org.kwakmunsu.haruhana.domain.submission.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.kwakmunsu.haruhana.domain.submission.entity.Submission;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionJpaRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByMemberIdAndDailyProblemIdAndStatus(Long memberId, Long dailyProblemId, EntityStatus status);

    @Query("""
            SELECT s FROM Submission s
            WHERE s.id = :submissionId
            AND s.status = :status
            """)
    Optional<Submission> findByIdAndStatus(@Param("submissionId") Long submissionId, @Param("status") EntityStatus status);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.dailyProblem dp
            JOIN FETCH dp.problem p
            WHERE s.id = :submissionId
            AND s.status = :status
            """)
    Optional<Submission> findWithProblemById(@Param("submissionId") Long submissionId, @Param("status") EntityStatus status);

    @Query("""
            SELECT DISTINCT s.dailyProblem.assignedAt
            FROM Submission s
            WHERE s.member.id = :memberId
              AND s.dailyProblem.assignedAt BETWEEN :startDate AND :endDate
              AND s.isOnTime = true
              AND s.status = :status
            """
    )
    List<LocalDate> findOnTimeDatesByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") EntityStatus status
    );

    @Modifying
    @Query("UPDATE Submission s SET s.status = :status, s.updatedAt = :now WHERE s.member.id = :memberId AND s.status = 'ACTIVE'")
    void softDeleteByMemberId(@Param("memberId") Long memberId, @Param("status") EntityStatus status, @Param("now") LocalDateTime now);

    long countByDailyProblem_AssignedAtAndIsOnTimeAndStatus(
            LocalDate assignedAt,
            boolean isOnTime,
            EntityStatus status
    );

}