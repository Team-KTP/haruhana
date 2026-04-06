package org.kwakmunsu.haruhana.domain.dailyproblem.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.kwakmunsu.haruhana.domain.dailyproblem.entity.DailyProblem;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyProblemJpaRepository extends JpaRepository<DailyProblem, Long> {

    @Query("""
            SELECT dp
            FROM DailyProblem dp
            JOIN FETCH dp.problem p
            JOIN FETCH p.categoryTopic ct
            WHERE dp.member.id = :memberId
              AND dp.assignedAt = :assignedAt
              AND dp.status = :status
            """
    )
    List<DailyProblem> findAllByMemberIdAndAssignedAtAndStatus(
            @Param("memberId") Long memberId,
            @Param("assignedAt") LocalDate assignedAt,
            @Param("status") EntityStatus status
    );

    @Query("""
            SELECT dp
            FROM DailyProblem dp
            JOIN FETCH dp.problem p
            JOIN FETCH p.categoryTopic ct
            WHERE dp.id = :id
              AND dp.member.id = :memberId
              AND dp.status = :status
            """
    )
    Optional<DailyProblem> findByIdAndMemberIdAndStatus(
            @Param("id") Long id,
            @Param("memberId") Long memberId,
            @Param("status") EntityStatus status
    );

    @Modifying
    @Query("UPDATE DailyProblem dp SET dp.status = :status, dp.updatedAt = :now WHERE dp.member.id = :memberId AND dp.status = 'ACTIVE'")
    void softDeleteByMemberId(@Param("memberId") Long memberId, @Param("status") EntityStatus status, @Param("now") LocalDateTime now);

    @Query("""
            SELECT dp.member.id
            FROM DailyProblem dp
            WHERE dp.assignedAt = :targetDate
              AND dp.isSolved = false
              AND dp.status = :status
            """)
    List<Long> findUnsolvedMemberIds(
            @Param("targetDate") LocalDate targetDate,
            @Param("status") EntityStatus status
    );

}