package org.kwakmunsu.haruhana.domain.dailyproblem.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.dailyproblem.entity.DailyProblem;
import org.kwakmunsu.haruhana.domain.dailyproblem.repository.DailyProblemJpaRepository;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class DailyProblemReader {

    private final DailyProblemJpaRepository dailyProblemJpaRepository;

    @Transactional(readOnly = true)
    public List<DailyProblem> findDailyProblemsByMember(Long memberId) {
        return dailyProblemJpaRepository.findAllByMemberIdAndAssignedAtAndStatus(
                memberId,
                LocalDate.now(),
                EntityStatus.ACTIVE
        );
    }

    public DailyProblem find(Long id, Long memberId) {
        return dailyProblemJpaRepository.findByIdAndMemberIdAndStatus(id, memberId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_DAILY_PROBLEM));
    }

    @Transactional(readOnly = true)
    public List<DailyProblem> findDailyProblems(LocalDate assignedAt, Long memberId) {
        if (assignedAt == null) {
            assignedAt = LocalDate.now();
        }

        return dailyProblemJpaRepository.findAllByMemberIdAndAssignedAtAndStatus(
                memberId,
                assignedAt,
                EntityStatus.ACTIVE
        );
    }

    public List<Long> findUnsolvedMember(LocalDate targetDate) {
        return dailyProblemJpaRepository.findUnsolvedMemberIds(targetDate, EntityStatus.ACTIVE);
    }

}