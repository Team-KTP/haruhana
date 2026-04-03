package org.kwakmunsu.haruhana.domain.member.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.category.service.CategoryReader;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.member.repository.MemberJpaRepository;
import org.kwakmunsu.haruhana.domain.member.repository.MemberPreferenceJpaRepository;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.NewPreference;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.NewProfile;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.UpdatePreference;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class MemberManager {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberPreferenceJpaRepository memberPreferenceJpaRepository;
    private final CategoryReader categoryReader;
    private final PasswordEncoder passwordEncoder;

    public Member create(NewProfile newProfile) {
        Member member = memberJpaRepository.save(Member.createMember(
                newProfile.loginId(),
                passwordEncoder.encode(newProfile.password()),
                newProfile.nickname(),
                Role.ROLE_MEMBER
        ));
        log.info("[MemberManager] 회원 생성 완료");

        return member;
    }

    @Transactional
    public MemberPreference registerPreference(Member member, NewPreference newPreference) {
        CategoryTopic categoryTopic = categoryReader.findCategoryTopic(newPreference.categoryTopicId());

        MemberPreference preference = memberPreferenceJpaRepository.save(MemberPreference.create(
                member,
                categoryTopic,
                newPreference.difficulty(),
                LocalDate.now() // 첫 등록은 등록 날짜로 고정
        ));
        log.info("[MemberManager] 회원 선호 설정 등록 완료 - categoryTopicId={}, difficulty={}",
                newPreference.categoryTopicId(), newPreference.difficulty());
        return preference;
    }

    @Transactional
    public void updatePreference(MemberPreference memberPreference, UpdatePreference updatePreference) {
        Member member = memberPreference.getMember();
        CategoryTopic categoryTopic = categoryReader.findCategoryTopic(updatePreference.categoryTopicId());

        // 이미 내일 날짜로 예약된 설정이 있으면 인플레이스 수정, 아니면 소프트 삭제 후 내일 날짜로 재생성
        if (memberPreference.isScheduledForTomorrow()) {
            memberPreference.updatePreference(categoryTopic, updatePreference.difficulty());
        } else {
            // 오늘 기준 설정 변경 시 기존 설정을 삭제하고 내일부터 적용되는 새로운 설정을 생성
            preferenceUpdateForTomorrow(memberPreference, member, categoryTopic, updatePreference);
        }

        log.info("[MemberManager] 회원 학습 목록 수정 - category: {}, difficulty: {} ",
                updatePreference.categoryTopicId(), updatePreference.difficulty());
    }

    public void updateRefreshToken(Member member, String refreshToken) {
        member.updateRefreshToken(refreshToken);
    }

    public void clearMember(Member member) {
        member.clearRefreshToken();
    }


    private void preferenceUpdateForTomorrow(
            MemberPreference memberPreference,
            Member member,
            CategoryTopic categoryTopic,
            UpdatePreference updatePreference
    ) {
        memberPreference.delete();
        memberPreferenceJpaRepository.flush(); // soft delete 즉시 반영

        memberPreferenceJpaRepository.save(MemberPreference.create(
                member,
                categoryTopic,
                updatePreference.difficulty(),
                LocalDate.now().plusDays(1)
        ));
    }
}