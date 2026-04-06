package org.kwakmunsu.haruhana.domain.problem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.global.entity.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Problem extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Lob
    @Column(nullable = false)
    private String aiAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private CategoryTopic categoryTopic;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProblemDifficulty difficulty;

    @Column(nullable = false)
    private LocalDate problemAt;

    @Column(nullable = false)
    private String promptVersion;

    public static Problem create(
            String title,
            String description,
            String aiAnswer,
            CategoryTopic categoryTopic,
            ProblemDifficulty difficulty,
            LocalDate problemAt,
            String promptVersion
    ) {
        Problem problem = new Problem();

        problem.title = title;
        problem.description = description;
        problem.aiAnswer = aiAnswer;
        problem.categoryTopic = categoryTopic;
        problem.difficulty = difficulty;
        problem.problemAt = problemAt;
        problem.promptVersion = promptVersion;

        return problem;
    }

}