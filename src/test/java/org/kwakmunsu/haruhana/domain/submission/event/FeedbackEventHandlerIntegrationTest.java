package org.kwakmunsu.haruhana.domain.submission.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.IntegrationTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryFactory;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryTopicJpaRepository;
import org.kwakmunsu.haruhana.domain.dailyproblem.entity.DailyProblem;
import org.kwakmunsu.haruhana.domain.dailyproblem.repository.DailyProblemJpaRepository;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.member.repository.MemberJpaRepository;
import org.kwakmunsu.haruhana.domain.problem.entity.Problem;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.domain.problem.repository.ProblemJpaRepository;
import org.kwakmunsu.haruhana.domain.submission.repository.SubmissionFeedbackJpaRepository;
import org.kwakmunsu.haruhana.domain.submission.repository.SubmissionJpaRepository;
import org.kwakmunsu.haruhana.domain.submission.service.dto.FeedbackGradingResult;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.kwakmunsu.haruhana.global.support.notification.ErrorNotificationSender;
import org.kwakmunsu.haruhana.infrastructure.gemini.GradingAiAdapter;
import org.kwakmunsu.haruhana.domain.submission.service.SubmissionService;
import org.kwakmunsu.haruhana.domain.streak.service.StreakService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * FeedbackEventHandler 통합 테스트
 * - 제출 커밋 후 AFTER_COMMIT 이벤트로 @Async 채점 흐름 검증
 * - GradingAiAdapter를 Mock하여 실제 AI 호출 없이 피드백 저장 여부 확인
 *
 * 주의: @TransactionalEventListener(AFTER_COMMIT)은 트랜잭션 커밋 후에 발생하므로
 * 테스트 클래스에 @Transactional을 사용하면 롤백으로 인해 이벤트가 발생하지 않음.
 * 따라서 @Transactional 없이 실제 커밋이 발생하도록 구성.
 */
@RequiredArgsConstructor
class FeedbackEventHandlerIntegrationTest extends IntegrationTestSupport {

    final CategoryFactory categoryFactory;
    final CategoryTopicJpaRepository categoryTopicJpaRepository;
    final MemberJpaRepository memberJpaRepository;
    final ProblemJpaRepository problemJpaRepository;
    final DailyProblemJpaRepository dailyProblemJpaRepository;
    final SubmissionService submissionService;
    final SubmissionJpaRepository submissionRepository;
    final SubmissionFeedbackJpaRepository feedbackRepository;

    @MockitoBean
    GradingAiAdapter gradingAiAdapter;

    @MockitoBean
    StreakService streakService;

    @MockitoBean
    ErrorNotificationSender errorNotificationSender;

    @BeforeEach
    void setUp() {
        // FK 제약 순서를 고려한 역방향 삭제 (자식 → 부모)
        feedbackRepository.deleteAll();
        submissionRepository.deleteAll();
        dailyProblemJpaRepository.deleteAll();
        problemJpaRepository.deleteAll();
        memberJpaRepository.deleteAll();
        categoryFactory.deleteAll();
        categoryFactory.saveAll();
    }

    @Test
    void 제출_커밋_후_비동기_AI_채점이_완료되면_피드백이_저장된다() {
        // given
        var member = memberJpaRepository.save(MemberFixture.createMemberWithOutId(Role.ROLE_MEMBER));

        var categoryTopic = categoryTopicJpaRepository.findByName("Java")
                .orElseThrow(() -> new RuntimeException("Java 토픽이 존재하지 않습니다"));

        var problem = problemJpaRepository.save(Problem.create(
                "테스트 문제",
                "테스트 설명",
                "AI 모범 답안",
                categoryTopic,
                ProblemDifficulty.MEDIUM,
                LocalDate.now(),
                "V1_PROMPT"
        ));

        var dailyProblem = dailyProblemJpaRepository.save(createDailyProblemFixture(member, problem));

        given(gradingAiAdapter.grade(any(), any(), any()))
                .willReturn(new FeedbackGradingResult("GOOD", "핵심 개념 명확히 설명", "동작 원리 설명 부족", "트랜잭션 전파 속성 추가 언급"));

        // when - 트랜잭션 커밋 발생 → AFTER_COMMIT 이벤트 → @Async 채점 핸들러 실행
        var response = submissionService.submitSolution(dailyProblem.getId(), member.getId(), "사용자 답변");

        // then - @Async 처리 완료 대기 (최대 5초)
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    var feedbacks = feedbackRepository.findBySubmissionIdAndStatusOrderByCreatedAtDesc(
                            response.submissionId(), EntityStatus.ACTIVE
                    );
                    assertThat(feedbacks).hasSize(1);
                });
    }

    @Test
    void AI_채점_실패_시_피드백이_저장되지_않고_에러_알림이_전송된다() {
        // given
        var member = memberJpaRepository.save(MemberFixture.createMemberWithOutId(Role.ROLE_MEMBER));

        var categoryTopic = categoryTopicJpaRepository.findByName("Java")
                .orElseThrow(() -> new RuntimeException("Java 토픽이 존재하지 않습니다"));

        var problem = problemJpaRepository.save(Problem.create(
                "테스트 문제",
                "테스트 설명",
                "AI 모범 답안",
                categoryTopic,
                ProblemDifficulty.MEDIUM,
                LocalDate.now(),
                "V1_PROMPT"
        ));

        var dailyProblem = dailyProblemJpaRepository.save(createDailyProblemFixture(member, problem));

        given(gradingAiAdapter.grade(any(), any(), any()))
                .willThrow(new HaruHanaException(ErrorType.FAIL_TO_GRADE_SUBMISSION));

        // when - 제출 자체는 정상 커밋됨
        var response = submissionService.submitSolution(dailyProblem.getId(), member.getId(), "사용자 답변");

        // then - AsyncUncaughtExceptionHandler 가 예외를 처리하여 에러 알림 전송
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    var feedbacks = feedbackRepository.findBySubmissionIdAndStatusOrderByCreatedAtDesc(
                            response.submissionId(), EntityStatus.ACTIVE
                    );
                    assertThat(feedbacks).isEmpty();
                    verify(errorNotificationSender, atLeastOnce()).sendErrorNotification(any(), any());
                });
    }

    @Test
    void 재제출_시에도_AI_채점이_수행된다() {
        // given
        var member = memberJpaRepository.save(MemberFixture.createMemberWithOutId(Role.ROLE_MEMBER));

        var categoryTopic = categoryTopicJpaRepository.findByName("Java")
                .orElseThrow(() -> new RuntimeException("Java 토픽이 존재하지 않습니다"));

        var problem = problemJpaRepository.save(Problem.create(
                "테스트 문제",
                "테스트 설명",
                "AI 모범 답안",
                categoryTopic,
                ProblemDifficulty.MEDIUM,
                LocalDate.now(),
                "V1_PROMPT"
        ));

        var dailyProblem = dailyProblemJpaRepository.save(createDailyProblemFixture(member, problem));

        given(gradingAiAdapter.grade(any(), any(), any()))
                .willReturn(new FeedbackGradingResult("EXCELLENT", "매우 상세한 설명", "없음", "더 다양한 예시 추가"));

        // when - 첫 번째 제출
        var firstResponse = submissionService.submitSolution(dailyProblem.getId(), member.getId(), "첫 번째 답변");

        // 첫 번째 채점 완료 대기
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(
                        feedbackRepository.findBySubmissionIdAndStatusOrderByCreatedAtDesc(
                                firstResponse.submissionId(), EntityStatus.ACTIVE)
                ).hasSize(1));

        // when - 재제출 (같은 submissionId)
        submissionService.submitSolution(dailyProblem.getId(), member.getId(), "수정된 답변");

        // then - 재제출에 대한 채점도 수행되어 피드백 2개
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(
                        feedbackRepository.findBySubmissionIdAndStatusOrderByCreatedAtDesc(
                                firstResponse.submissionId(), EntityStatus.ACTIVE)
                ).hasSize(2));
    }

    private DailyProblem createDailyProblemFixture(Member member, Problem problem) {
        return DailyProblem.create(member, problem, LocalDate.now());
    }

}
