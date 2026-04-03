package org.kwakmunsu.haruhana.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.UnitTestSupport;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.NewPreference;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.UpdatePreference;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.domain.problem.service.ProblemGenerator;
import org.kwakmunsu.haruhana.global.support.image.StorageProvider;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class MemberServiceUnitTest extends UnitTestSupport {

    @Mock
    MemberReader memberReader;

    @Mock
    MemberManager memberManager;

    @Mock
    MemberValidator memberValidator;

    @Mock
    MemberRemover memberRemover;

    @Mock
    ProblemGenerator problemGenerator;

    @Mock
    StorageProvider storageProvider;

    @InjectMocks
    MemberService memberService;

    @Test
    void 회원_탈퇴에_성공한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);

        // when
        memberService.withdraw(member.getId());

        // then
        verify(memberRemover, times(1)).remove(member.getId());
    }

    @Test
    void 회원_프로필을_조회한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var spring = MemberPreference.create(member, CategoryTopic.create(1L, "spring"), ProblemDifficulty.EASY, LocalDate.now());
        var network = MemberPreference.create(member, CategoryTopic.create(2L, "네트워크"), ProblemDifficulty.EASY, LocalDate.now());

        member.updateProfileImageObjectKey("profile-image-key");
        var profileImageUrl = "https://presigned-url.com/profile-image";

        given(memberReader.getMemberPreferences(member.getId())).willReturn(List.of(spring, network));
        given(storageProvider.generatePresignedReadUrl(member.getProfileImageObjectKey())).willReturn(profileImageUrl);

        // when
        var memberProfileResponse = memberService.getProfile(member.getId());

        // then
        assertThat(memberProfileResponse).isNotNull()
                .extracting(
                        MemberProfileResponse::loginId,
                        MemberProfileResponse::nickname,
                        MemberProfileResponse::profileImageUrl
                ).containsExactly(
                        member.getLoginId(),
                        member.getNickname(),
                        profileImageUrl
                );
        assertThat(memberProfileResponse.memberPreferences()).hasSize(2)
                .extracting(
                        MemberProfileResponse.MemberPreferenceResponse::categoryTopicName,
                        MemberProfileResponse.MemberPreferenceResponse::difficulty
                ).containsExactly(
                        tuple(
                                spring.getCategoryTopic().getName(),
                                spring.getDifficulty().name()
                        ),
                        tuple(
                                network.getCategoryTopic().getName(),
                                network.getDifficulty().name()
                        )
                );
    }

    @Test
    void 회원_학습_정보_변경에_성공한다() {
        // given
        var memberPreference = mock(MemberPreference.class);

        given(memberReader.getMemberPreference(any())).willReturn(memberPreference);
        given(memberPreference.isEqualsPreference(any(), any())).willReturn(false);

        var updatePreference = new UpdatePreference(2L, ProblemDifficulty.MEDIUM);

        // when
        memberService.updatePreference(updatePreference, 2L);

        // then
        verify(memberManager, times(1)).updatePreference(any(), any());
    }

    @Test
    void 변경_요청_데이터가_기존_학습_정보와_동일하다면_아무_일도_일어나지_않는다() {
        // given
        var memberPreference = mock(MemberPreference.class);

        given(memberReader.getMemberPreference(any())).willReturn(memberPreference);
        given(memberPreference.isEqualsPreference(any(Long.class), any(ProblemDifficulty.class))).willReturn(true);

        var updatePreference = new UpdatePreference(2L, ProblemDifficulty.MEDIUM);

        // when
        memberService.updatePreference(updatePreference, 2L);

        // then
        verify(memberManager, never()).updatePreference(any(), any());
    }

    @Test
    void 사용_가능한_로그인ID_인지_확인한다() {
        // given
        given(memberReader.existsByLoginId(any())).willReturn(false);

        // when
        boolean idAvailable = memberService.checkLoginIdAvailable("valid-login-id");

        // then
        assertThat(idAvailable).isTrue();
    }

    @Test
    void 이미_존재하는_로그인ID라면_사용_불가_결과를_반환한다() {
        // given
        given(memberReader.existsByLoginId(any())).willReturn(true);

        // when
        boolean idAvailable = memberService.checkLoginIdAvailable("duplicate-login-id");

        // then
        assertThat(idAvailable).isFalse();
    }

    @Test
    void 회원_학습_선호_정보를_추가한다() {
        // given
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var categoryTopic = CategoryTopic.create(1L, "Java");
        var memberPreference = MemberPreference.create(member, categoryTopic, ProblemDifficulty.MEDIUM, LocalDate.now());
        var newPreference = new NewPreference(1L, ProblemDifficulty.MEDIUM);

        given(memberReader.findWithLock(member.getId())).willReturn(member);
        given(memberManager.registerPreference(member, newPreference)).willReturn(memberPreference);

        // when
        memberService.appendPreference(newPreference, member.getId());

        // then
        verify(memberReader, times(1)).findWithLock(member.getId());
        verify(memberValidator, times(1)).validateAppendPreference(newPreference, member.getId());
        verify(memberManager, times(1)).registerPreference(member, newPreference);
        verify(problemGenerator, times(1)).generateInitialProblem(any(), any(), any());
    }

}