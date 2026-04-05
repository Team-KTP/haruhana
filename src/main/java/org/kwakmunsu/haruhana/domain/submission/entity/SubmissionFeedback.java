package org.kwakmunsu.haruhana.domain.submission.entity;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.kwakmunsu.haruhana.domain.submission.enums.FeedbackGrade;
import org.kwakmunsu.haruhana.global.entity.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class SubmissionFeedback extends BaseEntity {

    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Submission submission;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private FeedbackGrade grade;

    @Lob
    @Column(nullable = false)
    private String strengths;

    @Lob
    @Column(nullable = false)
    private String weaknesses;

    @Lob
    @Column(nullable = false)
    private String suggestion;

    @Column(nullable = false)
    private LocalDateTime gradedAt;

    public static SubmissionFeedback create(
            Submission submission,
            FeedbackGrade grade,
            String strengths,
            String weaknesses,
            String suggestion
    ) {
        SubmissionFeedback feedback = new SubmissionFeedback();

        feedback.submission = requireNonNull(submission);
        feedback.grade      = requireNonNull(grade);
        feedback.strengths  = requireNonNull(strengths);
        feedback.weaknesses = requireNonNull(weaknesses);
        feedback.suggestion = requireNonNull(suggestion);
        feedback.gradedAt   = LocalDateTime.now();

        return feedback;
    }

}