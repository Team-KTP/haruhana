package org.kwakmunsu.haruhana.domain.submission.repository;

import java.util.List;
import org.kwakmunsu.haruhana.domain.submission.entity.SubmissionFeedback;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionFeedbackJpaRepository extends JpaRepository<SubmissionFeedback, Long> {

    List<SubmissionFeedback> findBySubmissionIdAndStatusOrderByCreatedAtDesc(Long submissionId, EntityStatus status);

}