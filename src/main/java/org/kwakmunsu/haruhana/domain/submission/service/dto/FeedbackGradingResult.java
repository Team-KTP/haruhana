package org.kwakmunsu.haruhana.domain.submission.service.dto;

public record FeedbackGradingResult(
        String grade,
        String strengths,
        String weaknesses,
        String suggestion
) {

}