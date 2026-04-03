package org.kwakmunsu.haruhana.domain.member.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberPreferenceJpaRepository extends JpaRepository<MemberPreference, Long> {

    List<MemberPreference> findAllByEffectiveAtLessThanEqualAndStatus(LocalDate effectiveAt, EntityStatus status);

    @Query("SELECT mp FROM MemberPreference mp JOIN FETCH mp.categoryTopic WHERE mp.member.id = :memberId AND mp.status = :status")
    Optional<MemberPreference> findByMemberIdAndStatus(Long memberId, EntityStatus status);

    @Query("SELECT mp FROM MemberPreference mp JOIN FETCH mp.member m JOIN FETCH mp.categoryTopic WHERE m.id = :memberId AND mp.status = :status")
    List<MemberPreference> findAllByMemberIdWithMember(@Param("memberId") Long memberId, @Param("status") EntityStatus status);

    @Query("SELECT mp FROM MemberPreference mp JOIN FETCH mp.member m JOIN FETCH mp.categoryTopic WHERE m.id = :memberId AND mp.status = :status")
    Optional<MemberPreference> findByMemberIdWithMember(@Param("memberId") Long memberId, @Param("status") EntityStatus status);

    @Modifying
    @Query("UPDATE MemberPreference mp SET mp.status = :status, mp.updatedAt = :now WHERE mp.member.id = :memberId AND mp.status = 'ACTIVE'")
    void softDeleteByMemberId(@Param("memberId") Long memberId, @Param("status") EntityStatus status, @Param("now") LocalDateTime now);

    int countByMemberIdAndStatus(Long memberId, EntityStatus status);

    boolean existsByMemberIdAndCategoryTopicIdAndDifficultyAndStatus(Long memberId, Long aLong, ProblemDifficulty difficulty, EntityStatus entityStatus);
}