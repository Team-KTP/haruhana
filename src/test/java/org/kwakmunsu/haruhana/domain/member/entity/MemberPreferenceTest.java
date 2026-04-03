package org.kwakmunsu.haruhana.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;

class MemberPreferenceTest {

    @Test
    void effectiveAt이_내일_날짜이면_true를_반환한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopic.create(1L, "알고리즘");
        var memberPreference = MemberPreference.create(member, categoryTopic, ProblemDifficulty.MEDIUM, LocalDate.now().plusDays(1));

        // when
        var result = memberPreference.isScheduledForTomorrow();

        // then
        assertThat(result).isTrue();
    }

    @Test
    void effectiveAt이_오늘_날짜이면_false를_반환한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopic.create(1L, "알고리즘");
        var memberPreference = MemberPreference.create(member, categoryTopic, ProblemDifficulty.MEDIUM, LocalDate.now());

        // when
        var result = memberPreference.isScheduledForTomorrow();

        // then
        assertThat(result).isFalse();
    }

    @Test
    void effectiveAt이_과거_날짜이면_false를_반환한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopic.create(1L, "알고리즘");
        var memberPreference = MemberPreference.create(member, categoryTopic, ProblemDifficulty.MEDIUM, LocalDate.now().minusDays(1));

        // when
        var result = memberPreference.isScheduledForTomorrow();

        // then
        assertThat(result).isFalse();
    }

}
