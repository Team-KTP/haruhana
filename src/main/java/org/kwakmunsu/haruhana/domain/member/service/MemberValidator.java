package org.kwakmunsu.haruhana.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.repository.MemberJpaRepository;
import org.kwakmunsu.haruhana.domain.member.repository.MemberPreferenceJpaRepository;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.NewPreference;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.NewProfile;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.UpdateProfile;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MemberValidator {

    private static final int MAX_PREFERENCE_COUNT = 5;

    private final MemberJpaRepository memberJpaRepository;
    private final MemberPreferenceJpaRepository memberPreferenceJpaRepository;
    private final NicknameFilter nicknameFilter;

    public void validateNew(NewProfile newProfile) {
        if (memberJpaRepository.existsByLoginIdAndStatus(newProfile.loginId(), EntityStatus.ACTIVE)) {
            throw new HaruHanaException(ErrorType.DUPLICATE_LOGIN_ID);
        }
        validateNicknameAvailable(newProfile.nickname());
    }

    public void validateUpdateProfile(UpdateProfile updateProfile, Member member) {
        if (member.hasMatchingNickname(updateProfile.nickname())) {
            return;
        }

        nicknameFilter.validate(updateProfile.nickname());

        if (memberJpaRepository.existsByNicknameAndStatus(updateProfile.nickname(), EntityStatus.ACTIVE)) {
            throw new HaruHanaException(ErrorType.DUPLICATE_NICKNAME);
        }
    }

    public boolean isNicknameAvailable(String nickname) {
        try {
            nicknameFilter.validate(nickname);
            if (memberJpaRepository.existsByNicknameAndStatus(nickname, EntityStatus.ACTIVE)) {
                return false;
            }
        } catch (HaruHanaException e) {
            return false;
        }
        return true;
    }

    /**
     * 회원 선호 학습 정보 등록 시, 최대 등록 가능한 선호 학습 정보 개수와 중복 카테고리 존재 여부를 검증한다.
     *
     */
    public void validateAppendPreference(NewPreference newPreference, Long memberId) {
        int currentPreferenceCount = memberPreferenceJpaRepository.countByMemberIdAndStatus(memberId, EntityStatus.ACTIVE);
        if (currentPreferenceCount >= MAX_PREFERENCE_COUNT) {
            throw new HaruHanaException(ErrorType.EXCEED_MAX_PREFERENCE_COUNT);
        }

        boolean isExistingPreference = memberPreferenceJpaRepository.existsByMemberIdAndCategoryTopicIdAndDifficultyAndStatus(
                memberId,
                newPreference.categoryTopicId(),
                newPreference.difficulty(),
                EntityStatus.ACTIVE
        );
        if (isExistingPreference) {
            throw new HaruHanaException(ErrorType.DUPLICATE_PREFERENCE);
        }
    }

    private void validateNicknameAvailable(String nickname) {
        nicknameFilter.validate(nickname);
        if (memberJpaRepository.existsByNicknameAndStatus(nickname, EntityStatus.ACTIVE)) {
            throw new HaruHanaException(ErrorType.DUPLICATE_NICKNAME);
        }
    }
}