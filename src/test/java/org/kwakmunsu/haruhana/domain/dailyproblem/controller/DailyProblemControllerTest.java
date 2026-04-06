package org.kwakmunsu.haruhana.domain.dailyproblem.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.ControllerTestSupport;
import org.kwakmunsu.haruhana.domain.dailyproblem.controller.dto.SubmitSolutionRequest;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.DailyProblemDetailResponse;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.DailyProblemResponse;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.TodayProblemResponse;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.domain.submission.service.dto.response.SubmissionResponse;
import org.kwakmunsu.haruhana.security.annotation.TestMember;
import org.springframework.http.MediaType;

class DailyProblemControllerTest extends ControllerTestSupport {

    @TestMember
    @Test
    void 오늘의_문제를_조회한다() {
        // given
        var response = new TodayProblemResponse(
                1L,
                "Java의 equals와 hashCode",
                "Java에서 equals()와 hashCode()를 함께 재정의해야 하는 이유는?",
                "MEDIUM",
                "Java",
                false
        );

        given(dailyProblemService.getTodayProblem(any())).willReturn(List.of(response));

        // when & then
        assertThat(mvcTester.get().uri("/v1/daily-problem/today"))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("data[0].id", v -> v.assertThat().isEqualTo(response.id().intValue()))
                .hasPathSatisfying("data[0].title", v -> v.assertThat().isEqualTo(response.title()))
                .hasPathSatisfying("data[0].description", v -> v.assertThat().isEqualTo(response.description()))
                .hasPathSatisfying("data[0].difficulty", v -> v.assertThat().isEqualTo(response.difficulty()))
                .hasPathSatisfying("data[0].categoryTopicName", v -> v.assertThat().isEqualTo(response.categoryTopicName()))
                .hasPathSatisfying("data[0].isSolved", v -> v.assertThat().isEqualTo(response.isSolved()));
    }

    @TestMember
    @Test
    void 문제_상세를_조회한다_제출_정보_없음() {
        // given
        var dailyProblemId = 1L;
        var response = DailyProblemDetailResponse.builder()
                .id(dailyProblemId)
                .difficulty("MEDIUM")
                .categoryTopic("Java")
                .assignedAt(LocalDate.now())
                .title("Java의 equals와 hashCode")
                .description("Java에서 equals()와 hashCode()를 함께 재정의해야 하는 이유는?")
                .build();

        given(dailyProblemService.getDailyProblem(anyLong(), anyLong())).willReturn(response);

        // when & then
        assertThat(mvcTester.get().uri("/v1/daily-problem/{dailyProblemId}", dailyProblemId))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("data.id", v -> v.assertThat().isEqualTo(response.id().intValue()))
                .hasPathSatisfying("data.title", v -> v.assertThat().isEqualTo(response.title()))
                .hasPathSatisfying("data.difficulty", v -> v.assertThat().isEqualTo(response.difficulty()))
                .hasPathSatisfying("data.categoryTopic", v -> v.assertThat().isEqualTo(response.categoryTopic()))
                .hasPathSatisfying("data.aiAnswer", v -> v.assertThat().isNull())
                .hasPathSatisfying("data.userAnswer", v -> v.assertThat().isNull())
                .hasPathSatisfying("data.submittedAt", v -> v.assertThat().isNull());
    }

    @TestMember
    @Test
    void 문제_상세를_조회한다_제출_정보_있음() {
        // given
        var dailyProblemId = 1L;
        var submittedAt = LocalDateTime.now();
        var response = DailyProblemDetailResponse.builder()
                .id(dailyProblemId)
                .difficulty("MEDIUM")
                .categoryTopic("Java")
                .assignedAt(LocalDate.now())
                .title("Java의 equals와 hashCode")
                .description("Java에서 equals()와 hashCode()를 함께 재정의해야 하는 이유는?")
                .userAnswer("equals()를 재정의하면 hashCode()도 함께 재정의해야 합니다. 왜냐하면...")
                .submittedAt(submittedAt)
                .aiAnswer("equals()와 hashCode()는 객체의 동등성 비교에 사용됩니다...")
                .build();

        given(dailyProblemService.getDailyProblem(anyLong(), anyLong())).willReturn(response);

        // when & then
        assertThat(mvcTester.get().uri("/v1/daily-problem/{dailyProblemId}", dailyProblemId))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("data.id", v -> v.assertThat().isEqualTo(response.id().intValue()))
                .hasPathSatisfying("data.title", v -> v.assertThat().isEqualTo(response.title()))
                .hasPathSatisfying("data.userAnswer", v -> v.assertThat().isEqualTo(response.userAnswer()))
                .hasPathSatisfying("data.submittedAt", v -> v.assertThat().isNotNull());
    }

    @TestMember
    @Test
    void 문제_답안을_제출한다() throws JsonProcessingException {
        // given
        var dailyProblemId = 1L;
        var userAnswer = "equals()를 재정의하면 hashCode()도 함께 재정의해야 합니다. 왜냐하면...";
        var request = new SubmitSolutionRequest(userAnswer);
        var response = SubmissionResponse.builder()
                .submissionId(1L)
                .dailyProblemId(dailyProblemId)
                .userAnswer(userAnswer)
                .aiAnswer("equals()와 hashCode()는 객체의 동등성 비교에 사용됩니다...")
                .submittedAt(LocalDateTime.now())
                .build();

        given(submissionService.submitSolution(anyLong(), anyLong(), any(String.class)))
                .willReturn(response);

        // when & then
        assertThat(mvcTester.post().uri("/v1/daily-problem/{dailyProblemId}/submissions", dailyProblemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("data.submissionId", v -> v.assertThat().isEqualTo(response.submissionId().intValue()))
                .hasPathSatisfying("data.dailyProblemId", v -> v.assertThat().isEqualTo(response.dailyProblemId().intValue()))
                .hasPathSatisfying("data.userAnswer", v -> v.assertThat().isEqualTo(response.userAnswer()))
                .hasPathSatisfying("data.aiAnswer", v -> v.assertThat().isEqualTo(response.aiAnswer()))
                .hasPathSatisfying("data.submittedAt", v -> v.assertThat().isNotNull());
    }

    @TestMember
    @Test
    void 주어진_날짜에_할당된_문제를_조회한다() {
        // given
        DailyProblemResponse response = DailyProblemResponse.builder()
                .id(1L)
                .difficulty(ProblemDifficulty.MEDIUM.name())
                .categoryTopic("Java")
                .title("Java의 equals와 hashCode")
                .isSolved(false)
                .build();

        given(dailyProblemService.findDailyProblems(any(LocalDate.class), anyLong())).willReturn(List.of(response));

        // when & then
        assertThat(mvcTester.get().uri("/v1/daily-problem")
                .param("date", LocalDate.now().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("data[0].id", v -> v.assertThat().isEqualTo(response.id().intValue()))
                .hasPathSatisfying("data[0].difficulty", v -> v.assertThat().isEqualTo(response.difficulty()))
                .hasPathSatisfying("data[0].categoryTopic", v -> v.assertThat().isEqualTo(response.categoryTopic()))
                .hasPathSatisfying("data[0].title", v -> v.assertThat().isEqualTo(response.title()))
                .hasPathSatisfying("data[0].isSolved", v -> v.assertThat().isEqualTo(false));
    }

    @TestMember
    @Test
    void 여러_카테고리의_문제를_날짜로_조회한다() {
        // given
        DailyProblemResponse javaResponse = DailyProblemResponse.builder()
                .id(1L)
                .difficulty(ProblemDifficulty.MEDIUM.name())
                .categoryTopic("Java")
                .title("Java의 equals와 hashCode")
                .isSolved(false)
                .build();
        DailyProblemResponse springResponse = DailyProblemResponse.builder()
                .id(2L)
                .difficulty(ProblemDifficulty.EASY.name())
                .categoryTopic("Spring")
                .title("Spring IOC/DI")
                .isSolved(true)
                .build();

        given(dailyProblemService.findDailyProblems(any(LocalDate.class), anyLong()))
                .willReturn(List.of(javaResponse, springResponse));

        // when & then
        assertThat(mvcTester.get().uri("/v1/daily-problem")
                .param("date", LocalDate.now().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("data[0].id", v -> v.assertThat().isEqualTo(javaResponse.id().intValue()))
                .hasPathSatisfying("data[0].categoryTopic", v -> v.assertThat().isEqualTo(javaResponse.categoryTopic()))
                .hasPathSatisfying("data[1].id", v -> v.assertThat().isEqualTo(springResponse.id().intValue()))
                .hasPathSatisfying("data[1].categoryTopic", v -> v.assertThat().isEqualTo(springResponse.categoryTopic()));
    }

}