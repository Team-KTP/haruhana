package org.kwakmunsu.haruhana.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.ControllerTestSupport;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.controller.dto.DeviceTokenSyncRequest;
import org.kwakmunsu.haruhana.domain.member.controller.dto.PreferenceAppendRequest;
import org.kwakmunsu.haruhana.domain.member.controller.dto.PreferenceUpdateRequest;
import org.kwakmunsu.haruhana.domain.member.controller.dto.ProfileUpdateRequest;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.member.service.MemberProfileResponse;
import org.kwakmunsu.haruhana.domain.member.service.MemberProfileResponse.MemberPreferenceResponse;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.security.annotation.TestMember;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class MemberControllerTest extends ControllerTestSupport {

    @Test
    void 회원가입_Api를_요청한다() throws JsonProcessingException {
        // given
        var request = MemberFixture.createMemberCreateRequest();
        var jsonRequest = objectMapper.writeValueAsString(request);

        given(memberService.createMember(any(), any())).willReturn(1L);

        // when & then
        assertThat(mvcTester.post().uri("/v1/members/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .apply(print())
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.result", v -> v.assertThat().isEqualTo("SUCCESS"))
                .hasPathSatisfying("$.data", v -> v.assertThat().isEqualTo(1))
                .hasPathSatisfying("$.error", v -> v.assertThat().isNull());
    }

    @TestMember
    @Test
    void 회원_프로필_조회_Api를_요청한다() {
        // given
        var memberProfileResponse = new MemberProfileResponse(
                "loginId",
                "nickname",
                LocalDateTime.now(),
                Role.ROLE_MEMBER.name(),
                null,
                List.of(MemberPreferenceResponse.builder()
                        .preferenceId(1L)
                        .categoryTopicName("자료구조")
                        .difficulty(ProblemDifficulty.EASY.name())
                        .build()
                )
        );
        given(memberService.getProfile(any())).willReturn(memberProfileResponse);

        // when & then
        assertThat(mvcTester.get().uri("/v1/members"))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.data.loginId", v -> v.assertThat().isEqualTo(memberProfileResponse.loginId()))
                .hasPathSatisfying("$.data.nickname", v -> v.assertThat().isEqualTo(memberProfileResponse.nickname()))
                .hasPathSatisfying("$.data.memberPreferences[0].categoryTopicName",
                        v -> v.assertThat().isEqualTo(memberProfileResponse.memberPreferences().getFirst().categoryTopicName()))
                .hasPathSatisfying("$.data.memberPreferences[0].difficulty",
                        v -> v.assertThat().isEqualTo(memberProfileResponse.memberPreferences().getFirst().difficulty()))
                .hasPathSatisfying("$.data.role", v -> v.assertThat().isEqualTo(memberProfileResponse.role()));
    }

    @TestMember
    @Test
    void 회원_학습_정보를_변경한다() throws JsonProcessingException {
        // given
        var request = new PreferenceUpdateRequest(2L, ProblemDifficulty.MEDIUM);
        String requestJson = objectMapper.writeValueAsString(request);

        // when & then
        assertThat(mvcTester.patch().uri("/v1/members/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .apply(print())
                .hasStatusOk();
    }

    @TestMember
    @Test
    void 디바이스_토큰을_저장한다() throws JsonProcessingException {
        // given
        var request = new DeviceTokenSyncRequest("device-token-1234");
        String requestJson = objectMapper.writeValueAsString(request);

        // when & then
        assertThat(mvcTester.patch().uri("/v1/members/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .apply(print())
                .hasStatusOk();
    }

    @TestMember
    @Test
    void 회원_프로필을_업데이트_한다() throws JsonProcessingException {
        // given
        var profileUpdateRequest = new ProfileUpdateRequest("새닉네임", "profile-image-key-1234");
        String requestJson = objectMapper.writeValueAsString(profileUpdateRequest);

        // when
        assertThat(mvcTester.patch().uri("/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .apply(print())
                .hasStatus(HttpStatus.NO_CONTENT);

        verify(memberService, times(1)).updateProfile(any(), any());
    }

    @TestMember
    @Test
    void 회원_탈퇴_Api를_요청한다() {
        // when & then
        assertThat(mvcTester.delete().uri("/v1/members"))
                .apply(print())
                .hasStatus(HttpStatus.NO_CONTENT);

        verify(memberService, times(1)).withdraw(any());
    }

    @Test
    void 닉네임_사용_가능_여부_Api를_요청한다() {
        // given
        given(memberService.checkNicknameAvailable(any())).willReturn(true);

        // when
        assertThat(mvcTester.get().uri("/v1/members/nickname")
                .param("nickname", "사용가능한닉네임"))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.result", v -> v.assertThat().isEqualTo("SUCCESS"))
                .hasPathSatisfying("$.data", v -> v.assertThat().isEqualTo(true))
                .hasPathSatisfying("$.error", v -> v.assertThat().isNull());

        // then
        verify(memberService, times(1)).checkNicknameAvailable("사용가능한닉네임");
    }

    @Test
    void 로그인_사용_가능_여부_Api를_요청한다() {
        // given
        given(memberService.checkLoginIdAvailable(any())).willReturn(true);
        // when
        assertThat(mvcTester.get().uri("/v1/members/login-id")
                .param("loginId", "사용가능한LoginId"))
                .apply(print())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.result", v -> v.assertThat().isEqualTo("SUCCESS"))
                .hasPathSatisfying("$.data", v -> v.assertThat().isEqualTo(true))
                .hasPathSatisfying("$.error", v -> v.assertThat().isNull());

        // then
        verify(memberService, times(1)).checkLoginIdAvailable("사용가능한LoginId");
    }

    @TestMember
    @Test
    void 회원_학습_선호_정보_추가_Api를_요청한다() throws JsonProcessingException {
        // given
        var request = new PreferenceAppendRequest(1L, ProblemDifficulty.MEDIUM);
        String requestJson = objectMapper.writeValueAsString(request);

        // when & then
        assertThat(mvcTester.post().uri("/v1/members/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .apply(print())
                .hasStatusOk();

        verify(memberService, times(1)).appendPreference(any(), any());
    }

    @TestMember
    @Test
    void categoryTopicId가_null이면_400_에러를_반환한다() throws JsonProcessingException {
        // given
        var request = new PreferenceAppendRequest(null, ProblemDifficulty.MEDIUM);
        String requestJson = objectMapper.writeValueAsString(request);

        // when & then
        assertThat(mvcTester.post().uri("/v1/members/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .apply(print())
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @TestMember
    @Test
    void difficulty가_null이면_400_에러를_반환한다() throws JsonProcessingException {
        // given
        var request = new PreferenceAppendRequest(1L, null);
        String requestJson = objectMapper.writeValueAsString(request);

        // when & then
        assertThat(mvcTester.post().uri("/v1/members/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .apply(print())
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

}