package org.kwakmunsu.haruhana.domain.dailyproblem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.UnitTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryTopicFixture;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.dailyproblem.DailyProblemFixture;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.DailyProblemDetailResponse;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.DailyProblemResponse;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.TodayProblemResponse;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.problem.ProblemFixture;
import org.kwakmunsu.haruhana.domain.submission.SubmissionFixture;
import org.kwakmunsu.haruhana.domain.submission.service.SubmissionReader;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class DailyProblemServiceUnitTest extends UnitTestSupport {

    @Mock
    DailyProblemReader dailyProblemReader;

    @Mock
    SubmissionReader submissionReader;

    @InjectMocks
    DailyProblemService dailyProblemService;

    @Test
    void 오늘의_문제를_조회한다() {
        // given
        var memberId = 1L;
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopicFixture.createCategoryTopic();
        var problem = ProblemFixture.createProblem(categoryTopic);
        var dailyProblem = DailyProblemFixture.createUnsolvedDailyProblem(1L, member, problem);

        given(dailyProblemReader.findDailyProblemsByMember(memberId)).willReturn(List.of(dailyProblem));

        // when
        var responses = dailyProblemService.getTodayProblem(memberId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst()).extracting(
                TodayProblemResponse::id,
                TodayProblemResponse::title,
                TodayProblemResponse::description,
                TodayProblemResponse::difficulty,
                TodayProblemResponse::categoryTopicName,
                TodayProblemResponse::isSolved
        ).containsExactly(
                1L,
                ProblemFixture.TITLE,
                ProblemFixture.DESCRIPTION,
                ProblemFixture.DIFFICULTY.name(),
                CategoryTopicFixture.CATEGORY_TOPIC_NAME,
                false
        );

        verify(dailyProblemReader, times(1)).findDailyProblemsByMember(memberId);
    }

    @Test
    void 오늘의_문제가_여러_카테고리인_경우_모두_반환한다() {
        // given
        var memberId = 1L;
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var javaDailyProblem = DailyProblemFixture.createUnsolvedDailyProblem(1L, member,
                ProblemFixture.createProblem(CategoryTopicFixture.createCategoryTopic()));
        var springDailyProblem = DailyProblemFixture.createUnsolvedDailyProblem(2L, member,
                ProblemFixture.createProblem(2L, CategoryTopic.create(2L, "Spring")));

        given(dailyProblemReader.findDailyProblemsByMember(memberId))
                .willReturn(List.of(javaDailyProblem, springDailyProblem));

        // when
        var responses = dailyProblemService.getTodayProblem(memberId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(TodayProblemResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void 오늘의_문제가_없으면_빈_리스트를_반환한다() {
        // given
        var memberId = 1L;

        given(dailyProblemReader.findDailyProblemsByMember(memberId)).willReturn(List.of());

        // when
        var responses = dailyProblemService.getTodayProblem(memberId);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    void 문제_상세를_조회한다_제출_정보_없음() {
        // given
        var dailyProblemId = 1L;
        var memberId = 1L;
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopicFixture.createCategoryTopic();
        var problem = ProblemFixture.createProblem(categoryTopic);
        var dailyProblem = DailyProblemFixture.createUnsolvedDailyProblem(dailyProblemId, member, problem);

        given(dailyProblemReader.find(dailyProblemId, memberId)).willReturn(dailyProblem);
        given(submissionReader.findByMemberIdAndDailyProblemId(memberId, dailyProblemId))
                .willReturn(Optional.empty());

        // when
        DailyProblemDetailResponse response = dailyProblemService.getDailyProblem(dailyProblemId, memberId);

        // then
        assertThat(response).isNotNull().extracting(
                DailyProblemDetailResponse::id,
                DailyProblemDetailResponse::title,
                DailyProblemDetailResponse::description,
                DailyProblemDetailResponse::difficulty,
                DailyProblemDetailResponse::categoryTopic,
                DailyProblemDetailResponse::aiAnswer,
                DailyProblemDetailResponse::userAnswer,
                DailyProblemDetailResponse::submittedAt
        ).containsExactly(
                1L,
                ProblemFixture.TITLE,
                ProblemFixture.DESCRIPTION,
                ProblemFixture.DIFFICULTY.name(),
                CategoryTopicFixture.CATEGORY_TOPIC_NAME,
                null, // 제출 전에는 AI 답변을 볼 수 없음
                null,
                null
        );

        verify(dailyProblemReader, times(1)).find(dailyProblemId, memberId);
        verify(submissionReader, times(1)).findByMemberIdAndDailyProblemId(memberId, dailyProblemId);
    }

    @Test
    void 문제_상세를_조회한다_제출_정보_있음() {
        // given
        var dailyProblemId = 1L;
        var memberId = 1L;
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopicFixture.createCategoryTopic();
        var problem = ProblemFixture.createProblem(categoryTopic);
        var dailyProblem = DailyProblemFixture.createSolvedDailyProblem(dailyProblemId, member, problem);
        var submission = SubmissionFixture.createSubmission(member, dailyProblem);

        given(dailyProblemReader.find(dailyProblemId, memberId))
                .willReturn(dailyProblem);
        given(submissionReader.findByMemberIdAndDailyProblemId(memberId, dailyProblemId))
                .willReturn(Optional.of(submission));

        // when
        DailyProblemDetailResponse response = dailyProblemService.getDailyProblem(dailyProblemId, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userAnswer()).isEqualTo(SubmissionFixture.USER_ANSWER);
        assertThat(response.submittedAt()).isNotNull();
        assertThat(response.aiAnswer()).isEqualTo(ProblemFixture.AI_ANSWER); // 제출 후에는 AI 답변을 볼 수 있음

        verify(dailyProblemReader, times(1)).find(dailyProblemId, memberId);
        verify(submissionReader, times(1)).findByMemberIdAndDailyProblemId(memberId, dailyProblemId);
    }

    @Test
    void 다른_회원의_문제를_조회하면_예외를_발생시킨다() {
        // given
        Long dailyProblemId = 1L;
        Long memberId = 1L;

        given(dailyProblemReader.find(dailyProblemId, memberId))
                .willThrow(new HaruHanaException(ErrorType.NOT_FOUND_DAILY_PROBLEM));

        // when & then
        assertThatThrownBy(() -> dailyProblemService.getDailyProblem(dailyProblemId, memberId))
                .isInstanceOf(HaruHanaException.class)
                .hasMessage(ErrorType.NOT_FOUND_DAILY_PROBLEM.getMessage());
    }

    @Test
    void 날짜에_해당하는_할당된_문제를_조회한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var dailyProblem = DailyProblemFixture.createDailyProblem(
                member,
                ProblemFixture.createProblem(CategoryTopic.create(1L, "Java"))
        );

        given(dailyProblemReader.findDailyProblems(any(), any())).willReturn(List.of(dailyProblem));

        // when
        var responses = dailyProblemService.findDailyProblems(dailyProblem.getAssignedAt(), 1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst()).extracting(
                DailyProblemResponse::id,
                DailyProblemResponse::difficulty,
                DailyProblemResponse::categoryTopic,
                DailyProblemResponse::title,
                DailyProblemResponse::isSolved
        ).containsExactly(
                dailyProblem.getId(),
                dailyProblem.getProblem().getDifficulty().name(),
                dailyProblem.getProblem().getCategoryTopic().getName(),
                dailyProblem.getProblem().getTitle(),
                dailyProblem.isSolved()
        );
    }

    @Test
    void 날짜에_여러_카테고리_문제가_있을_경우_모두_반환한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var dailyProblem1 = DailyProblemFixture.createDailyProblem(1L, member,
                ProblemFixture.createProblem(CategoryTopic.create(1L, "Java")));
        var dailyProblem2 = DailyProblemFixture.createDailyProblem(2L, member,
                ProblemFixture.createProblem(2L, CategoryTopic.create(2L, "Spring")));

        given(dailyProblemReader.findDailyProblems(any(), any())).willReturn(List.of(dailyProblem1, dailyProblem2));

        // when
        var responses = dailyProblemService.findDailyProblems(LocalDate.now(), 1L);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(DailyProblemResponse::id)
                .containsExactly(dailyProblem1.getId(), dailyProblem2.getId());
    }

    @Test
    void 날짜에_해당된_문제가_없다() {
        // given
        given(dailyProblemReader.findDailyProblems(any(), any())).willReturn(List.of());

        // when
        var responses = dailyProblemService.findDailyProblems(LocalDate.now(), 1L);

        // then
        assertThat(responses).isEmpty();
    }

}