package org.kwakmunsu.haruhana.domain.submission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.ControllerTestSupport;
import org.kwakmunsu.haruhana.domain.submission.enums.FeedbackGrade;
import org.kwakmunsu.haruhana.domain.submission.service.dto.response.FeedbackResponse;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.kwakmunsu.haruhana.security.annotation.TestMember;

class SubmissionControllerTest extends ControllerTestSupport {

    @TestMember
    @Test
    void 피드백_목록을_조회한다() {
        // given
        var submissionId = 1L;
        var feedbacks = List.of(
                new FeedbackResponse(1L, FeedbackGrade.GOOD, "핵심 개념 명확히 설명", "동작 원리 설명 부족", "트랜잭션 전파 속성 추가 언급", LocalDateTime.now()),
                new FeedbackResponse(2L, FeedbackGrade.EXCELLENT, "매우 상세한 설명", "없음", "더 다양한 예시 추가", LocalDateTime.now())
        );

        given(feedbackGradingService.getFeedbacks(anyLong(), anyLong())).willReturn(feedbacks);

        // when & then
        assertThat(mvcTester.get().uri("/v1/submissions/{submissionId}/feedbacks", submissionId))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("data[0].feedbackId", v -> v.assertThat().isEqualTo(1))
                .hasPathSatisfying("data[0].grade", v -> v.assertThat().isEqualTo("GOOD"))
                .hasPathSatisfying("data[0].strengths", v -> v.assertThat().isEqualTo("핵심 개념 명확히 설명"))
                .hasPathSatisfying("data[1].feedbackId", v -> v.assertThat().isEqualTo(2))
                .hasPathSatisfying("data[1].grade", v -> v.assertThat().isEqualTo("EXCELLENT"));
    }

    @TestMember
    @Test
    void 피드백이_없으면_빈_목록을_반환한다() {
        // given
        given(feedbackGradingService.getFeedbacks(anyLong(), anyLong())).willReturn(List.of());

        // when & then
        assertThat(mvcTester.get().uri("/v1/submissions/{submissionId}/feedbacks", 1L))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("data", v -> v.assertThat().isEqualTo(List.of()));
    }

    @TestMember
    @Test
    void 다른_회원의_피드백_조회_시_403을_반환한다() {
        // given
        given(feedbackGradingService.getFeedbacks(anyLong(), anyLong()))
                .willThrow(new HaruHanaException(ErrorType.FORBIDDEN_ERROR));

        // when & then
        assertThat(mvcTester.get().uri("/v1/submissions/{submissionId}/feedbacks", 1L))
                .apply(print())
                .hasStatus(403);
    }

}
