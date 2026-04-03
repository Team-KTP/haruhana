package org.kwakmunsu.haruhana.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
public class MemberPreference extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    private CategoryTopic categoryTopic;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProblemDifficulty difficulty;

    @Column(nullable = false)
    private LocalDate effectiveAt;

    public static MemberPreference create(
            Member member,
            CategoryTopic categoryTopic,
            ProblemDifficulty difficulty,
            LocalDate effectiveAt
    ) {
        MemberPreference memberPreference = new MemberPreference();

        memberPreference.member = member;
        memberPreference.categoryTopic = categoryTopic;
        memberPreference.difficulty = difficulty;
        memberPreference.effectiveAt = effectiveAt;

        return memberPreference;
    }

    // NOTE: 같은 날짜에 회원 설정 변경 시 기존 설정을 업데이트하는 방식으로 처리
    public void updatePreference(
            CategoryTopic categoryTopic,
            ProblemDifficulty difficulty
    ) {
        this.categoryTopic = categoryTopic;
        this.difficulty = difficulty;
    }

    public boolean isEqualsPreference(Long categoryTopicId, ProblemDifficulty difficulty) {
        return (this.categoryTopic.getId().equals(categoryTopicId) && this.difficulty == difficulty);
    }

    public boolean isScheduledForTomorrow() {
        return this.effectiveAt.isEqual(LocalDate.now().plusDays(1));
    }

}