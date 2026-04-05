package org.kwakmunsu.haruhana.domain.submission.service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import org.kwakmunsu.haruhana.domain.submission.entity.SubmissionFeedback;
import org.kwakmunsu.haruhana.domain.submission.enums.FeedbackGrade;

@Builder(access = AccessLevel.PRIVATE)
@Schema(description = "AI 채점 결과 응답 DTO")
public record FeedbackResponse(
        @Schema(description = "채점 ID", example = "1")
        Long feedbackId,

        @Schema(description = "채점 등급", example = "GOOD")
        FeedbackGrade grade,

        @Schema(description = "잘한 점", example = "핵심 개념을 명확히 정의하고...")
        String strengths,

        @Schema(description = "부족한 점", example = "동작 원리에 대한 설명이 부족하며...")
        String weaknesses,

        @Schema(description = "개선 방향", example = "트랜잭션 전파 속성과 격리 수준을 함께 언급하면...")
        String suggestion,

        @Schema(description = "채점 완료 일시")
        LocalDateTime gradedAt
) {

    public static FeedbackResponse from(SubmissionFeedback feedback) {
        return FeedbackResponse.builder()
                .feedbackId(feedback.getId())
                .grade(feedback.getGrade())
                .strengths(feedback.getStrengths())
                .weaknesses(feedback.getWeaknesses())
                .suggestion(feedback.getSuggestion())
                .gradedAt(feedback.getCreatedAt())
                .build();
    }

}