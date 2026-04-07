package org.kwakmunsu.haruhana.domain.dailyproblem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.IntegrationTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryTopicFixture;
import org.kwakmunsu.haruhana.domain.dailyproblem.DailyProblemFixture;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.problem.ProblemFixture;
import org.kwakmunsu.haruhana.domain.streak.service.StreakManager;
import org.kwakmunsu.haruhana.domain.submission.SubmissionFixture;
import org.kwakmunsu.haruhana.domain.submission.service.FeedbackGradingService;
import org.kwakmunsu.haruhana.domain.submission.service.SubmissionManager;
import org.kwakmunsu.haruhana.domain.submission.service.SubmissionReader;
import org.kwakmunsu.haruhana.domain.submission.service.SubmissionService;
import org.kwakmunsu.haruhana.domain.submission.service.dto.response.SubmissionResult;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * DailyProblem 캐시 통합 테스트
 * <p>
 * Spring AOP 프록시를 통해 @Cacheable / @CacheEvict 가 실제로 동작하는지 검증.
 * 단위 테스트(@InjectMocks)는 프록시를 거치지 않으므로 반드시 통합 테스트로 검증한다.
 */
@RequiredArgsConstructor
class DailyProblemCacheTest extends IntegrationTestSupport {

    final DailyProblemService dailyProblemService;
    final SubmissionService submissionService;
    final CacheManager cacheManager;

    @MockitoBean
    DailyProblemReader dailyProblemReader;

    @MockitoBean
    SubmissionReader submissionReader;

    @MockitoBean
    SubmissionManager submissionManager;

    // 캐시 무효화 테스트 범위를 벗어나는 비동기 이벤트 체인을 차단
    @MockitoBean
    ApplicationEventPublisher eventPublisher;

    // 비동기 이벤트 핸들러(SubmissionEventHandler)가 DB에 접근하지 않도록 차단
    @MockitoBean
    StreakManager streakManager;

    // @MockitoBean ApplicationEventPublisher는 ApplicationContext 자체가 주입될 수 있어
    // 실제 이벤트가 발행될 수 있음 → FeedbackGradingService를 직접 차단
    @MockitoBean
    FeedbackGradingService feedbackGradingService;

    @AfterEach
    void clearCache() {
        Objects.requireNonNull(cacheManager.getCache("todayProblem")).clear();
        Objects.requireNonNull(cacheManager.getCache("dailyProblemDetail")).clear();
    }

    // ──────────────────────────────────────────────────────────────
    // todayProblem 캐시
    // ──────────────────────────────────────────────────────────────

    @Test
    void 오늘의_문제_두_번_조회_시_DB는_한_번만_호출된다() {
        // given
        var memberId = 1L;
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var problem = ProblemFixture.createProblem(CategoryTopicFixture.createCategoryTopic());
        var dailyProblem = DailyProblemFixture.createUnsolvedDailyProblem(1L, member, problem);

        given(dailyProblemReader.findDailyProblemsByMember(memberId)).willReturn(List.of(dailyProblem));

        // when: 동일 회원으로 2번 조회
        dailyProblemService.getTodayProblem(memberId);
        dailyProblemService.getTodayProblem(memberId);

        // then: DB는 1번만 호출
        verify(dailyProblemReader, times(1)).findDailyProblemsByMember(memberId);
    }

    @Test
    void 오늘의_문제_캐시_키에_날짜가_포함된다() {
        // given
        var memberId = 1L;
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var problem = ProblemFixture.createProblem(CategoryTopicFixture.createCategoryTopic());
        var dailyProblem = DailyProblemFixture.createUnsolvedDailyProblem(1L, member, problem);

        given(dailyProblemReader.findDailyProblemsByMember(memberId)).willReturn(List.of(dailyProblem));

        // when
        dailyProblemService.getTodayProblem(memberId);

        // then: memberId:날짜 형태의 키로 캐시에 저장
        var expectedKey = memberId + ":" + LocalDate.now();
        assertThat(Objects.requireNonNull(cacheManager.getCache("todayProblem")).get(expectedKey)).isNotNull();
    }

    @Test
    void 다른_회원의_오늘의_문제_캐시는_독립적으로_관리된다() {
        // given
        var memberIdA = 1L;
        var memberIdB = 2L;
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var problem = ProblemFixture.createProblem(CategoryTopicFixture.createCategoryTopic());

        given(dailyProblemReader.findDailyProblemsByMember(memberIdA))
                .willReturn(List.of(DailyProblemFixture.createUnsolvedDailyProblem(1L, member, problem)));
        given(dailyProblemReader.findDailyProblemsByMember(memberIdB))
                .willReturn(List.of(DailyProblemFixture.createUnsolvedDailyProblem(2L, member, problem)));

        // when: 각각 2번씩 조회
        dailyProblemService.getTodayProblem(memberIdA);
        dailyProblemService.getTodayProblem(memberIdA);
        dailyProblemService.getTodayProblem(memberIdB);
        dailyProblemService.getTodayProblem(memberIdB);

        // then: 각 회원마다 DB는 1번씩만 호출
        verify(dailyProblemReader, times(1)).findDailyProblemsByMember(memberIdA);
        verify(dailyProblemReader, times(1)).findDailyProblemsByMember(memberIdB);
    }

    @Test
    void 문제_제출_후_오늘의_문제_캐시가_무효화된다() {
        // given: 캐시 채우기
        var dailyProblemId = 1L;
        var memberId = 1L;
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var problem = ProblemFixture.createProblem(CategoryTopicFixture.createCategoryTopic());
        var dailyProblem = DailyProblemFixture.createUnsolvedDailyProblem(dailyProblemId, member, problem);

        given(dailyProblemReader.findDailyProblemsByMember(memberId)).willReturn(List.of(dailyProblem));
        dailyProblemService.getTodayProblem(memberId);

        var cache = cacheManager.getCache("todayProblem");
        assertThat(Objects.requireNonNull(cache).get(memberId + ":" + LocalDate.now())).isNotNull();

        // given: 제출 모킹
        var submission = SubmissionFixture.createSubmission(member, dailyProblem);
        given(dailyProblemReader.find(dailyProblemId, memberId)).willReturn(dailyProblem);
        given(submissionManager.submit(any(), any())).willReturn(new SubmissionResult(submission, true));

        // when: 문제 제출
        submissionService.submitSolution(dailyProblemId, memberId, "answer");

        // then: 캐시 무효화
        assertThat(cache.get(memberId + ":" + LocalDate.now())).isNull();

        // then: 재조회 시 DB 재호출
        dailyProblemService.getTodayProblem(memberId);
        verify(dailyProblemReader, times(2)).findDailyProblemsByMember(memberId);
    }

    // ──────────────────────────────────────────────────────────────
    // dailyProblemDetail 캐시
    // ──────────────────────────────────────────────────────────────

    @Test
    void 문제_상세_두_번_조회_시_DB는_한_번만_호출된다() {
        // given
        var dailyProblemId = 1L;
        var memberId = 1L;
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var problem = ProblemFixture.createProblem(CategoryTopicFixture.createCategoryTopic());
        var dailyProblem = DailyProblemFixture.createUnsolvedDailyProblem(dailyProblemId, member, problem);

        given(dailyProblemReader.find(dailyProblemId, memberId)).willReturn(dailyProblem);
        given(submissionReader.findByMemberIdAndDailyProblemId(memberId, dailyProblemId)).willReturn(Optional.empty());

        // when: 동일 키로 2번 조회
        dailyProblemService.getDailyProblem(dailyProblemId, memberId);
        dailyProblemService.getDailyProblem(dailyProblemId, memberId);

        // then: DB는 1번만 호출
        verify(dailyProblemReader, times(1)).find(dailyProblemId, memberId);
        verify(submissionReader, times(1)).findByMemberIdAndDailyProblemId(memberId, dailyProblemId);
    }

    @Test
    void 문제_제출_후_문제_상세_캐시가_무효화된다() {
        // given: 캐시 채우기
        var dailyProblemId = 1L;
        var memberId = 1L;
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var problem = ProblemFixture.createProblem(CategoryTopicFixture.createCategoryTopic());
        var dailyProblem = DailyProblemFixture.createUnsolvedDailyProblem(dailyProblemId, member, problem);

        given(dailyProblemReader.find(dailyProblemId, memberId)).willReturn(dailyProblem);
        given(submissionReader.findByMemberIdAndDailyProblemId(memberId, dailyProblemId)).willReturn(Optional.empty());
        dailyProblemService.getDailyProblem(dailyProblemId, memberId);

        var cache = cacheManager.getCache("dailyProblemDetail");
        assertThat(Objects.requireNonNull(cache).get(memberId + ":" + dailyProblemId)).isNotNull();

        // given: 제출 모킹
        var submission = SubmissionFixture.createSubmission(member, dailyProblem);
        given(submissionManager.submit(any(), any())).willReturn(new SubmissionResult(submission, true));

        // when: 문제 제출
        submissionService.submitSolution(dailyProblemId, memberId, "answer");

        // then: 캐시 무효화
        assertThat(cache.get(memberId + ":" + dailyProblemId)).isNull();

        // then: 재조회 시 DB 재호출
        // 총 3회: 1) getDailyProblem(캐시 미스), 2) submitSolution 내부 직접 호출, 3) 무효화 후 재조회
        dailyProblemService.getDailyProblem(dailyProblemId, memberId);
        verify(dailyProblemReader, times(3)).find(dailyProblemId, memberId);
    }

}
