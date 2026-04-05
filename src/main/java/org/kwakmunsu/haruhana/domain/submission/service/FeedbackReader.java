package org.kwakmunsu.haruhana.domain.submission.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.submission.entity.SubmissionFeedback;
import org.kwakmunsu.haruhana.domain.submission.repository.SubmissionFeedbackJpaRepository;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FeedbackReader {

    private final SubmissionFeedbackJpaRepository feedbackRepository;

    public List<SubmissionFeedback> findAllBySubmissionId(Long submissionId) {
        return feedbackRepository.findBySubmissionIdAndStatusOrderByCreatedAtDesc(submissionId, EntityStatus.ACTIVE);
    }

}