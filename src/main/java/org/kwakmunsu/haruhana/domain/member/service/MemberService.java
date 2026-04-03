package org.kwakmunsu.haruhana.domain.member.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.NewPreference;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.NewProfile;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.UpdatePreference;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.UpdateProfile;
import org.kwakmunsu.haruhana.domain.problem.service.ProblemGenerator;
import org.kwakmunsu.haruhana.domain.storage.service.StorageManager;
import org.kwakmunsu.haruhana.domain.streak.service.StreakManager;
import org.kwakmunsu.haruhana.global.support.image.StorageProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberService {

    // NOTE: 의존성이 너무 많이 엮여있는데 리팩토링이 필요해 보임
    private final MemberManager memberManager;
    private final MemberReader memberReader;
    private final MemberValidator memberValidator;
    private final MemberRemover memberRemover;
    private final ProblemGenerator problemGenerator;
    private final StreakManager streakManager;
    private final MemberDeviceManager memberDeviceManager;
    private final MemberDeviceValidator memberDeviceValidator;
    private final StorageManager storageManager;
    private final StorageProvider storageProvider;

    /**
     * 회원 가입 <br>
     * 회원 가입 시, 선호 학습 정보와 Streak 함께 등록 <br>
     * 회원 가입 시, 오늘의 문제 초기 문제 직접 생성 (Async 처리) <br>
     * @param newProfile 회원 가입 정보
     * @param newPreference 회원 선호 학습 정보
     * @return 생성된 회원의 식별자
     */
    @Transactional
    public Long createMember(NewProfile newProfile, NewPreference newPreference) {
        memberValidator.validateNew(newProfile);

        Member member = memberManager.create(newProfile);
        MemberPreference memberPreference = memberManager.registerPreference(member, newPreference);
        streakManager.create(member);
        // 회원가입 시에만 오늘의 문제 문제 직접 생성 - Async 처리
        problemGenerator.generateInitialProblem(member, memberPreference.getCategoryTopic(), memberPreference.getDifficulty());

        log.info("[MemberService] 회원 생성 및 학습 정보 등록 :{}, category: {}, difficulty: {}",
                member.getId(), memberPreference.getCategoryTopic().getName(), newPreference.difficulty());

        return member.getId();
    }

    public MemberProfileResponse getProfile(Long memberId) {
        // memberPreference를 통해 회원 정보와 선호 학습 정보를 함께 조회
        List<MemberPreference> memberPreferences = memberReader.getMemberPreferences(memberId);

        Member member = memberPreferences.getFirst().getMember();
        String profileImageObjectKey = member.getProfileImageObjectKey();
        String presignedReadUrl = profileImageObjectKey != null
                ? storageProvider.generatePresignedReadUrl(profileImageObjectKey)
                : null;

        return MemberProfileResponse.of(member, memberPreferences,  presignedReadUrl);
    }

    /**
     * 회원 프로필 업데이트 <br>
     * 회원 프로필 이미지가 변경된 경우 StorageManager를 통해 업로드 완료 처리 및 이전 이미지 S3 삭제 처리
     * @param updateProfile 회원 프로필 업데이트 정보
     * @param memberId 회원 식별자
     *
    * */
    @Transactional
    public void updateProfile(UpdateProfile updateProfile, Long memberId) {
        Member member = memberReader.find(memberId);
        memberValidator.validateUpdateProfile(updateProfile, member);

        storageManager.completeUpload(updateProfile.profileImageKey(), member);

        member.updateProfile(updateProfile.nickname());

        log.info("[MemberService] 프로필 업데이트 완료 - memberId: {}, hasProfileImage: {}",
                memberId, updateProfile.profileImageKey() != null);
    }

    /**
     * 회원 선호 학습 정보 추가 등록 <br>
     * 최대 등록 가능한 선호 학습 정보 개수와 중복 카테고리 존재 여부를 함께 검증합니다. <br>
     * 추가된 선호 학습 정보는 당일부터 적용됩니다. <br>
     * 추가된 선호 학습 정보에 해당하는 오늘의 문제를 직접 생성합니다. (Async 처리) <br>
     * @param newPreference 추가할 선호 학습 정보
     * @param memberId 회원 식별자
     * */
    @Transactional
    public void appendPreference(NewPreference newPreference, Long memberId) {
        Member member = memberReader.findWithLock(memberId);
        memberValidator.validateAppendPreference(newPreference, memberId);
        MemberPreference memberPreference = memberManager.registerPreference(member, newPreference);

        problemGenerator.generateInitialProblem(member, memberPreference.getCategoryTopic(), memberPreference.getDifficulty());

        log.info("[MemberService] 학습 선호도 추가 완료 - memberId: {}, categoryTopicId: {}, difficulty: {}",
                memberId, newPreference.categoryTopicId(), newPreference.difficulty());
    }

    /**
     * 회원 선호 학습 정보 업데이트 <br>
     * 기존 선호 학습 정보와 동일한 경우 업데이트하지 않고 로그만 남김
     * 변경된 학습 정보는 다음날부터 적용
    * */
    @Transactional
    public void updatePreference(UpdatePreference updatePreference, Long memberId) {
        MemberPreference memberPreference = memberReader.getMemberPreference(updatePreference.preferenceId(), memberId);

        // 기존 선호 학습 정보와 동일한 경우 업데이트하지 않음
        if (memberPreference.isEqualsPreference(updatePreference.categoryTopicId(), updatePreference.difficulty())) {
            log.debug("[MemberService] 선호도 변경 없음 - memberId: {}, preferenceId: {}", memberId, updatePreference.preferenceId());
            return;
        }

        memberManager.updatePreference(memberPreference, updatePreference);

        log.info("[MemberService] 학습 선호도 업데이트 완료 - memberId: {}, preferenceId: {} categoryTopicId: {}, difficulty: {}",
                memberId, updatePreference.preferenceId(), updatePreference.categoryTopicId(), updatePreference.difficulty());
    }

    /**
    * 회원 디바이스 토큰 동기화
    * @param memberId 회원 식별자
    * @param deviceToken 디바이스 토큰
    *
    **/
    public void syncDeviceTokens(Long memberId, String deviceToken) {
        memberDeviceManager.syncDeviceToken(memberId, deviceToken, LocalDateTime.now());

        log.info("[MemberService] 디바이스 토큰 동기화 완료 - memberId: {}", memberId);
    }

    /**
     * 회원 디바이스 토큰 삭제
     *
     * @param deviceToken 디바이스 토큰 - hard delete 처리
     * @param memberId 회원 식별자 - hard delete 처리
     **/
    public void deleteDeviceTokens(String deviceToken, Long memberId) {
        memberDeviceValidator.validateDeleteDeviceToken(deviceToken, memberId);

        memberDeviceManager.deleteDeviceToken(deviceToken, memberId);

        log.info("[MemberService] 디바이스 토큰 삭제 완료 - memberId: {}", memberId);
    }

    public void withdraw(Long memberId) {
        memberRemover.remove(memberId);

        log.info("[MemberService] 회원 탈퇴 완료 - memberId: {}", memberId);
    }

    /**
     * 닉네임 사용 가능 여부 검사 <br>
     * 부적절한 단어 포함 여부 및 중복 여부를 함께 검사합니다. <br>
     * @param nickname 검사할 닉네임
     * @return 사용 가능하면 true, 부적절한 단어 포함 또는 중복이면 false
     */
    public boolean checkNicknameAvailable(String nickname) {
       return memberValidator.isNicknameAvailable(nickname);
    }

    /**
     * 로그인 아이디 사용 가능 여부 검사 <br>
     * 중복 여부를 검사합니다. <br>
     * @param loginId 검사할 로그인 아이디
     * @return 사용 가능하면 true, 중복이면 false
    * */
    public boolean checkLoginIdAvailable(String loginId) {
        return !memberReader.existsByLoginId(loginId);
    }

}