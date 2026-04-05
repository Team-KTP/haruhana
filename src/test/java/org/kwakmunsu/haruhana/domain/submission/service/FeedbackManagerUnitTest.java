package org.kwakmunsu.haruhana.domain.submission.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.UnitTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryTopicFixture;
import org.kwakmunsu.haruhana.domain.dailyproblem.DailyProblemFixture;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.problem.ProblemFixture;
import org.kwakmunsu.haruhana.domain.submission.SubmissionFixture;
import org.kwakmunsu.haruhana.domain.submission.entity.SubmissionFeedback;
import org.kwakmunsu.haruhana.domain.submission.repository.SubmissionFeedbackJpaRepository;
import org.kwakmunsu.haruhana.domain.submission.service.dto.FeedbackGradingResult;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * FeedbackManager 유닛 테스트
 * - 채점 결과를 SubmissionFeedback 엔티티로 변환하여 저장하는 로직 검증
 */
class FeedbackManagerUnitTest extends UnitTestSupport {

    @Mock
    SubmissionFeedbackJpaRepository feedbackRepository;

    @InjectMocks
    FeedbackManager feedbackManager;

    @Test
    void 채점_결과를_SubmissionFeedback으로_변환하여_저장한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopicFixture.createCategoryTopic();
        var problem = ProblemFixture.createProblem(categoryTopic);
        var dailyProblem = DailyProblemFixture.createDailyProblem(member, problem);
        var submission = SubmissionFixture.createSubmission(member, dailyProblem);
        var result = new FeedbackGradingResult("GOOD", "핵심 개념을 잘 설명함", "세부 동작 원리 설명 부족", "트랜잭션 전파 속성 언급 추가");

        // when
        feedbackManager.save(submission, result);

        // then
        verify(feedbackRepository, times(1)).save(any(SubmissionFeedback.class));
    }

    @Test
    void 유효하지_않은_등급이면_예외가_발생한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopicFixture.createCategoryTopic();
        var problem = ProblemFixture.createProblem(categoryTopic);
        var dailyProblem = DailyProblemFixture.createDailyProblem(member, problem);
        var submission = SubmissionFixture.createSubmission(member, dailyProblem);
        var resultWithInvalidGrade = new FeedbackGradingResult("INVALID_GRADE", "강점", "약점", "제안");

        // when & then
        assertThatThrownBy(() -> feedbackManager.save(submission, resultWithInvalidGrade))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
