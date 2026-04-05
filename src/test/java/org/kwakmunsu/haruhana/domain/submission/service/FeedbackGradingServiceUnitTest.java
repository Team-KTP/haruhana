package org.kwakmunsu.haruhana.domain.submission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.UnitTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryTopicFixture;
import org.kwakmunsu.haruhana.domain.dailyproblem.DailyProblemFixture;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.problem.ProblemFixture;
import org.kwakmunsu.haruhana.domain.submission.SubmissionFixture;
import org.kwakmunsu.haruhana.domain.submission.entity.SubmissionFeedback;
import org.kwakmunsu.haruhana.domain.submission.enums.FeedbackGrade;
import org.kwakmunsu.haruhana.domain.submission.service.dto.FeedbackGradingResult;
import org.kwakmunsu.haruhana.domain.submission.service.dto.response.FeedbackResponse;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.kwakmunsu.haruhana.infrastructure.gemini.GradingAiAdapter;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * FeedbackGradingService 유닛 테스트
 * - AI 채점 수행 및 결과 저장 로직 검증
 * - 피드백 조회 및 소유권 검증 로직 검증
 */
class FeedbackGradingServiceUnitTest extends UnitTestSupport {

    @Mock
    SubmissionReader submissionReader;

    @Mock
    FeedbackManager feedbackManager;

    @Mock
    FeedbackReader feedbackReader;

    @Mock
    GradingAiAdapter gradingAiAdapter;

    @InjectMocks
    FeedbackGradingService feedbackGradingService;

    @Test
    void AI_채점을_수행하고_결과를_저장한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopicFixture.createCategoryTopic();
        var problem = ProblemFixture.createProblem(categoryTopic);
        var dailyProblem = DailyProblemFixture.createDailyProblem(member, problem);
        var submission = SubmissionFixture.createSubmission(member, dailyProblem);
        var gradingResult = new FeedbackGradingResult("GOOD", "핵심 개념 명확히 설명", "동작 원리 설명 부족", "트랜잭션 전파 속성 추가 언급");

        given(submissionReader.findWithProblem(submission.getId())).willReturn(submission);
        given(gradingAiAdapter.grade(any(), any(), any())).willReturn(gradingResult);

        // when
        feedbackGradingService.grade(submission.getId());

        // then
        verify(gradingAiAdapter, times(1)).grade(
                problem.getDescription(),
                problem.getAiAnswer(),
                submission.getAnswer()
        );
        verify(feedbackManager, times(1)).save(submission, gradingResult);
    }

    @Test
    void 본인의_제출이면_피드백_목록을_반환한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER); // id = 1L
        var categoryTopic = CategoryTopicFixture.createCategoryTopic();
        var problem = ProblemFixture.createProblem(categoryTopic);
        var dailyProblem = DailyProblemFixture.createDailyProblem(member, problem);
        var submission = SubmissionFixture.createSubmission(member, dailyProblem);
        var feedback = SubmissionFeedback.create(submission, FeedbackGrade.GOOD, "강점", "약점", "제안");

        given(submissionReader.findById(submission.getId())).willReturn(submission);
        given(feedbackReader.findAllBySubmissionId(submission.getId())).willReturn(List.of(feedback));

        // when
        var responses = feedbackGradingService.getFeedbacks(submission.getId(), member.getId());

        // then
        assertThat(responses)
                .hasSize(1)
                .extracting(FeedbackResponse::grade, FeedbackResponse::strengths, FeedbackResponse::weaknesses)
                .containsExactly(tuple(FeedbackGrade.GOOD, "강점", "약점"));
    }

    @Test
    void 피드백이_없으면_빈_목록을_반환한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopicFixture.createCategoryTopic();
        var problem = ProblemFixture.createProblem(categoryTopic);
        var dailyProblem = DailyProblemFixture.createDailyProblem(member, problem);
        var submission = SubmissionFixture.createSubmission(member, dailyProblem);

        given(submissionReader.findById(submission.getId())).willReturn(submission);
        given(feedbackReader.findAllBySubmissionId(submission.getId())).willReturn(List.of());

        // when
        var responses = feedbackGradingService.getFeedbacks(submission.getId(), member.getId());

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    void 다른_회원의_제출이면_피드백_조회_시_예외가_발생한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER); // id = 1L
        var categoryTopic = CategoryTopicFixture.createCategoryTopic();
        var problem = ProblemFixture.createProblem(categoryTopic);
        var dailyProblem = DailyProblemFixture.createDailyProblem(member, problem);
        var submission = SubmissionFixture.createSubmission(member, dailyProblem);
        var otherMemberId = 99L;

        given(submissionReader.findById(submission.getId())).willReturn(submission);

        // when & then
        assertThatThrownBy(() -> feedbackGradingService.getFeedbacks(submission.getId(), otherMemberId))
                .isInstanceOf(HaruHanaException.class);
    }

}
