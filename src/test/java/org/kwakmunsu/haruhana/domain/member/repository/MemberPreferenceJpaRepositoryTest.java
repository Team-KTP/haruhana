package org.kwakmunsu.haruhana.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.IntegrationTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryFactory;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryTopicJpaRepository;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
class MemberPreferenceJpaRepositoryTest extends IntegrationTestSupport {

    final MemberPreferenceJpaRepository memberPreferenceJpaRepository;
    final MemberJpaRepository memberJpaRepository;
    final CategoryFactory categoryFactory;
    final CategoryTopicJpaRepository categoryTopicJpaRepository;
    CategoryTopic categoryTopic;

    @BeforeEach
    void setUp() {
        categoryFactory.deleteAll();
        categoryFactory.saveAll();

        categoryTopic = categoryTopicJpaRepository.findByName("Java").orElseThrow();
    }

    @Test
    void findAllByMemberIdWithMember_JOIN_FETCH_확인() {
        // given
        var member = memberJpaRepository.save(MemberFixture.createMemberWithOutId(Role.ROLE_MEMBER));
        var memberPreference = MemberPreference.create(member, categoryTopic, ProblemDifficulty.MEDIUM, LocalDate.now());
        memberPreferenceJpaRepository.save(memberPreference);

        // when
        var foundMemberPreferences = memberPreferenceJpaRepository.findAllByMemberIdWithMember(member.getId(),
                EntityStatus.ACTIVE);

        // then
        assertThat(foundMemberPreferences).hasSize(1)
                .extracting(MemberPreference::getMember)
                .containsExactly(member);
    }

    @Test
    void findAllByMemberIdWithMember_다중_선호_학습_정보를_모두_반환한다() {
        // given
        var member = memberJpaRepository.save(MemberFixture.createMemberWithOutId(Role.ROLE_MEMBER));
        var javaCategory = categoryTopic;
        var springCategory = categoryTopicJpaRepository.findByName("Spring")
                .orElseThrow(() -> new RuntimeException("Spring 토픽이 존재하지 않습니다"));

        memberPreferenceJpaRepository.save(
                MemberPreference.create(member, javaCategory, ProblemDifficulty.MEDIUM, LocalDate.now()));
        memberPreferenceJpaRepository.save(
                MemberPreference.create(member, springCategory, ProblemDifficulty.HARD, LocalDate.now()));

        // when
        var foundMemberPreferences = memberPreferenceJpaRepository.findAllByMemberIdWithMember(member.getId(),
                EntityStatus.ACTIVE);

        // then
        assertThat(foundMemberPreferences).hasSize(2)
                .extracting(mp -> tuple(
                        mp.getCategoryTopic().getName(),
                        mp.getDifficulty()
                ))
                .containsExactlyInAnyOrder(
                        tuple("Java", ProblemDifficulty.MEDIUM),
                        tuple("Spring", ProblemDifficulty.HARD)
                );
    }

}