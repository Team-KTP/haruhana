package org.kwakmunsu.haruhana.domain.dailyproblem.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kwakmunsu.haruhana.domain.dailyproblem.entity.DailyProblem;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.DailyProblemDetailResponse;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.DailyProblemResponse;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.TodayProblemResponse;
import org.kwakmunsu.haruhana.domain.submission.entity.Submission;
import org.kwakmunsu.haruhana.domain.submission.service.SubmissionReader;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class DailyProblemService {

    private final DailyProblemReader dailyProblemReader;
    private final SubmissionReader submissionReader;

    /**
     * 오늘의 문제 조회
     *
     * @param memberId 회원 ID
     */
    @Cacheable(
            cacheNames = "todayProblem",
            key = "#memberId + ':' + T(java.time.LocalDate).now()",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<TodayProblemResponse> getTodayProblem(Long memberId) {
        log.debug("[DailyProblemService] 오늘의 문제 캐시 미스 - 조회 시작");
        List<DailyProblem> dailyProblems = dailyProblemReader.findDailyProblemsByMember(memberId);

        return dailyProblems.stream()
                .map(TodayProblemResponse::from)
                .toList();
    }

    /**
     * 문제 상세 조회
     *
     * @param dailyProblemId 오늘의 문제 ID
     * @param memberId       회원 ID
     */
    @Cacheable(cacheNames = "dailyProblemDetail", key = "#memberId + ':' + #dailyProblemId")
    @Transactional(readOnly = true)
    public DailyProblemDetailResponse getDailyProblem(Long dailyProblemId, Long memberId) {
        log.debug("[DailyProblemService] 문제 상세 캐시 미스 - 조회 시작, dailyProblemId={}", dailyProblemId);
        DailyProblem dailyProblem = dailyProblemReader.find(dailyProblemId, memberId);

        Submission submission = submissionReader.findByMemberIdAndDailyProblemId(memberId, dailyProblemId)
                .orElse(null);

        return DailyProblemDetailResponse.of(dailyProblem, submission);
    }

    /**
     * 날짜에 해당하는 회원에게 할당된 데일리 문제 미리보기 조회
     *
     * @param date     조회할 날짜
     * @param memberId 회원 ID returns DailyProblemResponse 데일리 문제 미리보기 응답 DTO
     *
     */
    public List<DailyProblemResponse> findDailyProblems(LocalDate date, Long memberId) {
        List<DailyProblem> dailyProblems = dailyProblemReader.findDailyProblems(date, memberId);

        return dailyProblems.stream()
                .map(DailyProblemResponse::from)
                .toList();
    }

}