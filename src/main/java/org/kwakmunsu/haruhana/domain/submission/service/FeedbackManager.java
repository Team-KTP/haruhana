package org.kwakmunsu.haruhana.domain.submission.service;

import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.submission.entity.Submission;
import org.kwakmunsu.haruhana.domain.submission.entity.SubmissionFeedback;
import org.kwakmunsu.haruhana.domain.submission.enums.FeedbackGrade;
import org.kwakmunsu.haruhana.domain.submission.repository.SubmissionFeedbackJpaRepository;
import org.kwakmunsu.haruhana.domain.submission.service.dto.FeedbackGradingResult;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FeedbackManager {

    private final SubmissionFeedbackJpaRepository feedbackRepository;

    public void save(Submission submission, FeedbackGradingResult result) {
        FeedbackGrade grade = FeedbackGrade.valueOf(result.grade().trim().toUpperCase());
        feedbackRepository.save(SubmissionFeedback.create(
                submission,
                grade,
                result.strengths(),
                result.weaknesses(),
                result.suggestion()
        ));
    }

}
