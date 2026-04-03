package org.kwakmunsu.haruhana.domain.member.service;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;

@Schema(description = "Member 프로필 응답 DTO")
@Builder
public record MemberProfileResponse(
        @Schema(description = "로그인 아이디", example = "haruhana123")
        String loginId,

        @Schema(description = "닉네임", example = "하루하나")
        String nickname,

        @Schema(description = "회원 가입 일시")
        LocalDateTime createdAt,

        @Schema(description = "회원 역할", example = "ROLE_MEMBER")
        String role,

        @Schema(description = "조회용 presignedUrl", example = "https://example.com/profile-image.jpg")
        String profileImageUrl,

        @Schema(description = "회원 선호 학습 정보 목록")
        List<MemberPreferenceResponse> memberPreferences
) {

    public static MemberProfileResponse of(Member member, List<MemberPreference> memberPreference, String profileImageUrl) {
        List<MemberPreferenceResponse> memberPreferenceResponses = memberPreference.stream()
                .map(MemberPreferenceResponse::from)
                .toList();

        return MemberProfileResponse.builder()
                .loginId(member.getLoginId())
                .nickname(member.getNickname())
                .createdAt(member.getCreatedAt())
                .role(member.getRole().name())
                .profileImageUrl(profileImageUrl)
                .memberPreferences(memberPreferenceResponses)
                .build();
    }

    @Builder
    public record MemberPreferenceResponse(
            @Schema(description = "학습 정보 id", example = "1")
            Long preferenceId,

            @Schema(description = "학습 카테고리 주제 이름", example = "자료구조")
            String categoryTopicName,

            @Schema(description = "학습 문제 난이도", example = "EASY")
            String difficulty
    ) {

        public static MemberPreferenceResponse from(MemberPreference memberPreference) {
            return MemberPreferenceResponse.builder()
                    .preferenceId(memberPreference.getId())
                    .categoryTopicName(memberPreference.getCategoryTopic().getName())
                    .difficulty(memberPreference.getDifficulty().name())
                    .build();
        }
    }

}