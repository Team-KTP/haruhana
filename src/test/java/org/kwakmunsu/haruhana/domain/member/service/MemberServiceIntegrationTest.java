package org.kwakmunsu.haruhana.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.IntegrationTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryFactory;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryTopicJpaRepository;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.member.repository.MemberJpaRepository;
import org.kwakmunsu.haruhana.domain.member.repository.MemberPreferenceJpaRepository;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.NewPreference;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.UpdateProfile;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.domain.problem.service.ProblemGenerator;
import org.kwakmunsu.haruhana.domain.storage.enums.UploadType;
import org.kwakmunsu.haruhana.domain.storage.repository.StorageJpaRepository;
import org.kwakmunsu.haruhana.domain.storage.service.StorageService;
import org.kwakmunsu.haruhana.domain.streak.service.StreakManager;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.kwakmunsu.haruhana.global.support.image.StorageProvider;
import org.kwakmunsu.haruhana.infrastructure.s3.dto.PresignedUrlResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * MemberService 통합 테스트 - registerPreference 전체 플로우 검증 - 실제 DB 사용 - 이벤트 처리 검증
 */
@RequiredArgsConstructor
@Transactional
class MemberServiceIntegrationTest extends IntegrationTestSupport {

    final CategoryFactory categoryFactory;
    final MemberService memberService;
    final MemberJpaRepository memberJpaRepository;
    final MemberPreferenceJpaRepository memberPreferenceJpaRepository;
    final CategoryTopicJpaRepository categoryTopicJpaRepository;
    final EntityManager entityManager;
    final StorageService storageService;
    final StorageJpaRepository storageJpaRepository;

    @MockitoBean
    StreakManager streakManager;

    @MockitoBean
    ProblemGenerator problemGenerator;

    @MockitoBean
    StorageProvider storageProvider;

    private CategoryTopic categoryTopic;

    @BeforeEach
    void setUp() {
        categoryFactory.deleteAll();
        categoryFactory.saveAll();

        categoryTopic = categoryTopicJpaRepository.findByName("Java")
                .orElseThrow(() -> new RuntimeException("Java 토픽이 존재하지 않습니다"));

        // ProblemGenerator의 generateInitialProblem을 Mock 처리 (실제 실행 방지)
        doNothing().when(problemGenerator).generateInitialProblem(any(), any(), any());
    }

    @Test
    void 회원_탈퇴_시_회원_및_연관_데이터가_삭제된다() {
        // given
        var newProfile = MemberFixture.createNewProfile();
        var newPreference = new NewPreference(categoryTopic.getId(), ProblemDifficulty.EASY);
        Long memberId = memberService.createMember(newProfile, newPreference);

        // when
        memberService.withdraw(memberId);
        entityManager.flush();
        entityManager.clear();

        // then
        var withdrawnMember = memberJpaRepository.findById(memberId).orElseThrow();
        assertThat(withdrawnMember.getStatus()).isEqualTo(EntityStatus.DELETED);

        var withdrawnPreference = memberPreferenceJpaRepository.findByMemberIdAndStatus(memberId, EntityStatus.DELETED).orElseThrow();
        assertThat(withdrawnPreference.getStatus()).isEqualTo(EntityStatus.DELETED);
    }

    @Test
    void 회원_탈퇴_시_loginId와_nickname이_익명화된다() {
        // given
        var newProfile = MemberFixture.createNewProfile();
        var newPreference = new NewPreference(categoryTopic.getId(), ProblemDifficulty.EASY);
        Long memberId = memberService.createMember(newProfile, newPreference);

        // when
        memberService.withdraw(memberId);
        entityManager.flush();
        entityManager.clear();

        // then
        var withdrawnMember = memberJpaRepository.findById(memberId).orElseThrow();
        assertThat(withdrawnMember.getLoginId()).startsWith("deleted_");
        assertThat(withdrawnMember.getNickname()).startsWith("deleted_");
        assertThat(withdrawnMember.getLoginId()).isNotEqualTo(MemberFixture.LOGIN_ID);
        assertThat(withdrawnMember.getNickname()).isNotEqualTo(MemberFixture.NICKNAME);
    }

    @Test
    void 탈퇴한_회원과_동일한_loginId와_nickname으로_재가입이_가능하다() {
        // given - 첫 번째 회원 가입 후 탈퇴
        var newProfile = MemberFixture.createNewProfile();
        var newPreference = new NewPreference(categoryTopic.getId(), ProblemDifficulty.EASY);
        Long firstMemberId = memberService.createMember(newProfile, newPreference);
        memberService.withdraw(firstMemberId);
        entityManager.flush();
        entityManager.clear();

        // when - 동일한 loginId, nickname으로 재가입
        Long secondMemberId = memberService.createMember(newProfile, newPreference);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(secondMemberId).isNotEqualTo(firstMemberId);
        var secondMember = memberJpaRepository.findById(secondMemberId).orElseThrow();
        assertThat(secondMember.getLoginId()).isEqualTo(MemberFixture.LOGIN_ID);
        assertThat(secondMember.getNickname()).isEqualTo(MemberFixture.NICKNAME);
        assertThat(secondMember.getStatus()).isEqualTo(EntityStatus.ACTIVE);
    }

    @Test
    void 회원가입_시_회원_학습_설정과_스트릭_생성_문제_생성에_성공한다() {
        // given
        var newProfile = MemberFixture.createNewProfile();
        var newPreference = new NewPreference(categoryTopic.getId(), ProblemDifficulty.EASY);

        // when
        Long memberId = memberService.createMember(newProfile, newPreference);

        // then
        assertThat(memberJpaRepository.findById(memberId).orElseThrow()).isNotNull();
        assertThat(memberPreferenceJpaRepository.findByMemberIdAndStatus(memberId, EntityStatus.ACTIVE).orElseThrow()).isNotNull();
        verify(streakManager, times(1)).create(any());
        verify(problemGenerator, times(1)).generateInitialProblem(any(), any(), any());
    }

    @Test
    void 회원_프로필중_닉네임만_변경된다() {
        // given
        var member = MemberFixture.createMemberWithOutId(Role.ROLE_MEMBER);
        memberJpaRepository.save(member);
        assertThat(member).extracting(
                Member::getNickname,
                Member::getProfileImageObjectKey
        ).containsExactly(
                member.getNickname(),
                null
        );

        // when
        var updateProfile = new UpdateProfile("새닉네임", null);
        memberService.updateProfile(updateProfile, member.getId());
        entityManager.flush();

        // then
        var updatedMember = memberJpaRepository.findById(member.getId()).orElseThrow();

        assertThat(updatedMember).extracting(
                Member::getNickname,
                Member::getProfileImageObjectKey
        ).containsExactly(
                updateProfile.nickname(),
                null
        );
    }

    @Test
    void 회원_프로필중_이미지만_변경된다() {
        // given
        var member = MemberFixture.createMemberWithOutId(Role.ROLE_MEMBER);
        memberJpaRepository.save(member);

        var mockPresignedUrlResponse = new PresignedUrlResponse("objectKey123", "http://presigned.url/upload");
        given(storageProvider.generatePresignedUploadUrl(any(), any())).willReturn(mockPresignedUrlResponse);

        // 프로필 이미지 업로드를 위한 presigned url 발급
        var presignedUrlResponse = storageService.createPresignedUrl("filename.png", UploadType.PROFILE_IMAGE, member.getId());
        // 검증
        assertThat(member).extracting(
                Member::getNickname,
                Member::getProfileImageObjectKey
        ).containsExactly(
                member.getNickname(),
                null
        );

        doNothing().when(storageProvider).ensureObjectExists(presignedUrlResponse.objectKey());

        // when
        var updateProfile = new UpdateProfile(member.getNickname(), presignedUrlResponse.objectKey());
        memberService.updateProfile(updateProfile, member.getId());

        // then
        assertThat(member).extracting(
                Member::getNickname,
                Member::getProfileImageObjectKey
        ).containsExactly(
                member.getNickname(),
                presignedUrlResponse.objectKey()
        );
        var storage = storageJpaRepository.findByMemberIdAndObjectKeyAndStatus(
                member.getId(),
                presignedUrlResponse.objectKey(),
                EntityStatus.ACTIVE
        ).orElseThrow();

        assertThat(storage.isComplete()).isTrue();
    }

    @Test
    void 학습_선호_정보를_추가한다() {
        // given
        var newProfile = MemberFixture.createNewProfile();
        var newPreference = new NewPreference(categoryTopic.getId(), ProblemDifficulty.EASY);
        Long memberId = memberService.createMember(newProfile, newPreference);

        var springTopic = categoryTopicJpaRepository.findByName("Spring")
                .orElseThrow(() -> new RuntimeException("Spring 토픽이 존재하지 않습니다"));
        var appendPreference = new NewPreference(springTopic.getId(), ProblemDifficulty.MEDIUM);

        // when
        memberService.appendPreference(appendPreference, memberId);

        // then
        var count = memberPreferenceJpaRepository.countByMemberIdAndStatus(memberId, org.kwakmunsu.haruhana.global.entity.EntityStatus.ACTIVE);
        assertThat(count).isEqualTo(2);
        // 선호 정보 추가 시 비동기로 초기 문제 생성
        verify(problemGenerator, times(2)).generateInitialProblem(any(), any(), any());
    }

    @Test
    void 최대_개수_초과_시_예외가_발생한다() {
        // given
        var newProfile = MemberFixture.createNewProfile();
        var newPreference = new NewPreference(categoryTopic.getId(), ProblemDifficulty.EASY);
        Long memberId = memberService.createMember(newProfile, newPreference);

        var springTopic = categoryTopicJpaRepository.findByName("Spring")
                .orElseThrow(() -> new RuntimeException("Spring 토픽이 존재하지 않습니다"));
        var mysqlTopic = categoryTopicJpaRepository.findByName("MySQL")
                .orElseThrow(() -> new RuntimeException("MySQL 토픽이 존재하지 않습니다"));
        var pythonTopic = categoryTopicJpaRepository.findByName("Python")
                .orElseThrow(() -> new RuntimeException("Python 토픽이 존재하지 않습니다"));
        var goTopic = categoryTopicJpaRepository.findByName("Go")
                .orElseThrow(() -> new RuntimeException("Go 토픽이 존재하지 않습니다"));
        var kotlinTopic = categoryTopicJpaRepository.findByName("Kotlin")
                .orElseThrow(() -> new RuntimeException("Kotlin 토픽이 존재하지 않습니다"));

        memberService.appendPreference(new NewPreference(springTopic.getId(), ProblemDifficulty.MEDIUM), memberId);
        memberService.appendPreference(new NewPreference(mysqlTopic.getId(), ProblemDifficulty.MEDIUM), memberId);
        memberService.appendPreference(new NewPreference(pythonTopic.getId(), ProblemDifficulty.MEDIUM), memberId);
        memberService.appendPreference(new NewPreference(goTopic.getId(), ProblemDifficulty.MEDIUM), memberId);

        // when & then
        var sixthPreference = new NewPreference(kotlinTopic.getId(), ProblemDifficulty.MEDIUM);
        assertThatThrownBy(() -> memberService.appendPreference(sixthPreference, memberId))
                .isInstanceOf(HaruHanaException.class)
                .hasMessage(ErrorType.EXCEED_MAX_PREFERENCE_COUNT.getMessage());
    }

    @Test
    void 중복_선호_정보_추가_시_예외가_발생한다() {
        // given
        var newProfile = MemberFixture.createNewProfile();
        var newPreference = new NewPreference(categoryTopic.getId(), ProblemDifficulty.EASY);
        Long memberId = memberService.createMember(newProfile, newPreference);

        var duplicatePreference = new NewPreference(categoryTopic.getId(), ProblemDifficulty.EASY);

        // when & then
        assertThatThrownBy(() -> memberService.appendPreference(duplicatePreference, memberId))
                .isInstanceOf(HaruHanaException.class)
                .hasMessage(ErrorType.DUPLICATE_PREFERENCE.getMessage());
    }

    @Test
    void 회원_프로필중_닉네임과_이미지가_변경된다() {
        // given
        var member = MemberFixture.createMemberWithOutId(Role.ROLE_MEMBER);
        memberJpaRepository.save(member);

        // Mock 설정 - 호출 순서대로 다른 objectKey 반환
        given(storageProvider.generatePresignedUploadUrl(any(), any()))
                .willReturn(
                        new PresignedUrlResponse("oldObjectKey123", "http://presigned.url/old"),
                        new PresignedUrlResponse("newObjectKey456", "http://presigned.url/new")
                );
        doNothing().when(storageProvider).ensureObjectExists(any());

        // 이전 프로필 이미지 업로드 및 적용
        var oldPresignedUrlResponse = storageService.createPresignedUrl("oldFilename.png", UploadType.PROFILE_IMAGE, member.getId());
        storageService.completeUpload(oldPresignedUrlResponse.objectKey(), member.getId());
        var oldUpdateProfile = new UpdateProfile(member.getNickname(), oldPresignedUrlResponse.objectKey());
        memberService.updateProfile(oldUpdateProfile, member.getId());

        // 이전 프로필 검증
        assertThat(member).extracting(
                Member::getNickname,
                Member::getProfileImageObjectKey
        ).containsExactly(
                member.getNickname(),
                oldPresignedUrlResponse.objectKey()
        );

        // 새로운 프로필 준비
        var newPresignedUrlResponse = storageService.createPresignedUrl("newFilename.png", UploadType.PROFILE_IMAGE, member.getId());
        var newNickname = "새로운닉네임";
        var newUpdateProfile = new UpdateProfile(newNickname, newPresignedUrlResponse.objectKey());

        // when - 닉네임과 이미지 모두 변경
        memberService.updateProfile(newUpdateProfile, member.getId());

        // then - 닉네임과 이미지가 모두 변경되었는지 확인
        assertThat(member).extracting(
                Member::getNickname,
                Member::getProfileImageObjectKey
        ).containsExactly(
                newNickname,
                newPresignedUrlResponse.objectKey()
        );

        // 새로운 Storage가 완료 상태인지 확인
        var newStorage = storageJpaRepository.findByMemberIdAndObjectKeyAndStatus(
                member.getId(),
                newPresignedUrlResponse.objectKey(),
                EntityStatus.ACTIVE
        ).orElseThrow();
        assertThat(newStorage.isComplete()).isTrue();
    }

}