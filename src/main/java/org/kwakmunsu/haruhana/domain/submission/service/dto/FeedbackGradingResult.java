package org.kwakmunsu.haruhana.domain.submission.service.dto;

import org.kwakmunsu.haruhana.domain.submission.enums.FeedbackGrade;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;

public record FeedbackGradingResult(
        String grade,
        String strengths,
        String weaknesses,
        String suggestion
) {

    public void validate() {
        if (grade == null || grade.isBlank()) {
            throw new HaruHanaException(ErrorType.FAIL_TO_GRADE_SUBMISSION);
        }

        try {
            FeedbackGrade.valueOf(grade.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new HaruHanaException(ErrorType.FAIL_TO_GRADE_SUBMISSION);
        }

        if (strengths == null || strengths.isBlank()) {
            throw new HaruHanaException(ErrorType.FAIL_TO_GRADE_SUBMISSION);
        }

        if (weaknesses == null || weaknesses.isBlank()) {
            throw new HaruHanaException(ErrorType.FAIL_TO_GRADE_SUBMISSION);
        }

        if (suggestion == null || suggestion.isBlank()) {
            throw new HaruHanaException(ErrorType.FAIL_TO_GRADE_SUBMISSION);
        }
    }

}