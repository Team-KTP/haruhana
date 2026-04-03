package org.kwakmunsu.haruhana.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.IntegrationTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryFactory;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryTopicJpaRepository;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.UpdatePreference;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
class MemberManagerIntegrationTest extends IntegrationTestSupport {

    final MemberManager memberManager;
    final MemberReader memberReader;
    final PasswordEncoder passwordEncoder;
    final CategoryFactory categoryFactory;
    final EntityManager entityManager;
    final CategoryTopicJpaRepository categoryTopicJpaRepository;

    CategoryTopic categoryTopic;

    @BeforeEach
    void setUpCategories() {
        categoryFactory.deleteAll();
        categoryFactory.saveAll();

        categoryTopic = categoryTopicJpaRepository.findByName("Java")
                .orElseThrow(() -> new RuntimeException("Java 토픽이 존재하지 않습니다"));
    }

    @Test
    void 회원을_생성한다() {
        // given
        var newProfile = MemberFixture.createNewProfile();

        // when
        var member = memberManager.create(newProfile);

        // then
        assertThat(member).isNotNull()
                .extracting(
                        Member::getLoginId,
                        Member::getNickname,
                        Member::getRole
                )
                .containsExactly(
                        newProfile.loginId(),
                        newProfile.nickname(),
                        Role.ROLE_MEMBER
                );
        assertThat(passwordEncoder.matches(newProfile.password(), member.getPassword())).isTrue();
    }

    @Test
    void 회원의_학습_정보를_등록한다() {
        // given
        var newProfile = MemberFixture.createNewProfile();
        var guest = memberManager.create(newProfile);
        var newPreference = MemberFixture.createNewPreference(categoryTopic.getId());

        // when
        var memberPreference = memberManager.registerPreference(guest, newPreference);
        entityManager.flush();

        // then
        assertThat(memberPreference).isNotNull()
                .extracting(
                        MemberPreference::getDifficulty,
                        MemberPreference::getEffectiveAt
                )
                .containsExactly(
                        newPreference.difficulty(),
                        LocalDate.now()
                );

        assertThat(guest.getRole()).isEqualTo(Role.ROLE_MEMBER);
        assertThat(memberPreference.getCategoryTopic().getId()).isEqualTo(newPreference.categoryTopicId());
    }

    @Test
    void 회원의_학습_정보를_변경한다() {
        // given
        var newProfile = MemberFixture.createNewProfile();
        var member = memberManager.create(newProfile);

        var newPreference = MemberFixture.createNewPreference(categoryTopic.getId());
        var memberPreference = memberManager.registerPreference(member, newPreference);
        entityManager.flush();

        var oldMemberPreference = memberReader.getMemberPreference(memberPreference.getId(), member.getId());

        assertThat(oldMemberPreference).isNotNull().extracting(
                MemberPreference::getCategoryTopic,
                MemberPreference::getDifficulty
        ).containsExactly(
                categoryTopic,
                newPreference.difficulty()
        );

        var springCategory = categoryTopicJpaRepository.findByName("Spring")
                .orElseThrow(() -> new RuntimeException("Spring 토픽이 존재하지 않습니다"));

        var updatePreference = new UpdatePreference(memberPreference.getId(), springCategory.getId(), ProblemDifficulty.HARD);

        // when
        memberManager.updatePreference(memberPreference, updatePreference);

        // then
        entityManager.flush();
        entityManager.clear();
        var newMemberPreference = memberReader.getMemberPreferences(member.getId()).stream()
                .filter(preference -> preference.getEffectiveAt().equals(LocalDate.now().plusDays(1)))
                .findFirst()
                .orElseThrow();
        assertThat(newMemberPreference).isNotNull()
                .extracting(
                        MemberPreference::getCategoryTopic,
                        MemberPreference::getDifficulty,
                        MemberPreference::getEffectiveAt
                )
                .containsExactly(
                        springCategory,
                        updatePreference.difficulty(),
                        LocalDate.now().plusDays(1)
                );
    }

    @Test
    void 같은_날짜에_학습_정보_변경_시_기존_엔티티를_업데이트한다() {
        // given
        var newProfile = MemberFixture.createNewProfile();
        var member = memberManager.create(newProfile);

        // 첫 등록
        var newPreference = MemberFixture.createNewPreference(categoryTopic.getId());
        var memberPreference = memberManager.registerPreference(member, newPreference);
        entityManager.flush();

        // 첫번쨰 업데이트
        var springCategory = categoryTopicJpaRepository.findByName("Spring")
                .orElseThrow(() -> new RuntimeException("Spring 토픽이 존재하지 않습니다"));
        var updatePreference = new UpdatePreference(memberPreference.getId(), springCategory.getId(), ProblemDifficulty.HARD);
        memberManager.updatePreference(memberPreference, updatePreference);

        // 같은 날짜에 두번째 업데이트 준비
        entityManager.flush();
        entityManager.clear();
        var newMemberPreference = memberReader.getMemberPreferences(member.getId()).stream()
                .filter(preference -> preference.getEffectiveAt().equals(LocalDate.now().plusDays(1)))
                .findFirst()
                .orElseThrow();
        var mysqlCategory = categoryTopicJpaRepository.findByName("MySQL")
                .orElseThrow(() -> new RuntimeException("MySQL 토픽이 존재하지 않습니다"));
        var updateSecPreference = new UpdatePreference(newMemberPreference.getId(), mysqlCategory.getId(), ProblemDifficulty.EASY);

        // when
        memberManager.updatePreference(newMemberPreference, updateSecPreference);

        // then
        entityManager.flush();
        entityManager.clear();
        var sameIdPreference = memberReader.getMemberPreference(newMemberPreference.getId(), member.getId());

        // 동일한 엔티티가 업데이트 되었는지 확인
        assertThat(sameIdPreference.getId()).isEqualTo(newMemberPreference.getId());
        assertThat(sameIdPreference).isNotNull()
                .extracting(
                        MemberPreference::getCategoryTopic,
                        MemberPreference::getDifficulty,
                        MemberPreference::getEffectiveAt
                )
                .containsExactly(
                        mysqlCategory,
                        updateSecPreference.difficulty(),
                        LocalDate.now().plusDays(1)
                );
    }

}