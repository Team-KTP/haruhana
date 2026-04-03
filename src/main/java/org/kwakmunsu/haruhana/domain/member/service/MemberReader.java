package org.kwakmunsu.haruhana.domain.member.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.member.repository.MemberJpaRepository;
import org.kwakmunsu.haruhana.domain.member.repository.MemberPreferenceJpaRepository;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.kwakmunsu.haruhana.global.security.jwt.TokenHasher;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MemberReader {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberPreferenceJpaRepository memberPreferenceJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public Member findByAccount(String loginId, String password) {
        Member member = memberJpaRepository.findByLoginIdAndStatus(loginId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new HaruHanaException(ErrorType.INVALID_ACCOUNT));

        member.validatePassword(passwordEncoder, password);

        return member;
    }

    public Member find(Long id) {
        return memberJpaRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_MEMBER));
    }

    public Member findWithLock(Long id) {
        return memberJpaRepository.findByIdAndStatusWithLock(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_MEMBER));
    }

    public long countAll() {
        return memberJpaRepository.countAllByStatus(EntityStatus.ACTIVE);
    }

    /**
     * 특정 날짜 범위에 제출 기록이 없는 회원들을 조회합니다.
     *
     * @param startOfDay 조회 시작 시간 (포함)
     * @param endOfDay   조회 종료 시간 (제외)
     * @return 제출 기록이 없는 활성 회원 목록
     */
    public List<Member> findMembersWithoutSubmissionBetween(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return memberJpaRepository.findMembersWithoutTodaySubmission(
                startOfDay,
                endOfDay,
                Role.ROLE_MEMBER,
                EntityStatus.ACTIVE
        );
    }

    public List<MemberPreference> getMemberPreferences(LocalDate targetDate) {
        return memberPreferenceJpaRepository.findAllByEffectiveAtLessThanEqualAndStatus(targetDate, EntityStatus.ACTIVE);
    }

    // NOTE: 삭제 예정 컴파일 에러를 방지하기 위해 남겨둔 메서드입니다. 추후 제거 예정입니다.
    public MemberPreference getMemberPreference(Long memberId) {
        return memberPreferenceJpaRepository.findByMemberIdWithMember(
                memberId,
                EntityStatus.ACTIVE
        ).orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_MEMBER_PREFERENCE));
    }

    public List<MemberPreference> getMemberPreferences(Long memberId) {
        List<MemberPreference> memberPreferences = memberPreferenceJpaRepository.findAllByMemberIdWithMember(
                memberId,
                EntityStatus.ACTIVE
        );
        if (memberPreferences.isEmpty()) {
            throw new HaruHanaException(ErrorType.NOT_FOUND_MEMBER_PREFERENCE);
        }
        return memberPreferences;
    }

    public Member findByRefreshToken(String refreshToken) {
        return memberJpaRepository.findByRefreshTokenAndStatus(TokenHasher.hash(refreshToken), EntityStatus.ACTIVE)
                .orElseThrow(() -> new HaruHanaException(ErrorType.NOT_FOUND_ACTIVE_MEMBER_BY_REFRESH_TOKEN));
    }

    public boolean existsByLoginId(String loginId) {
        return memberJpaRepository.existsByLoginIdAndStatus(loginId, EntityStatus.ACTIVE);
    }

}