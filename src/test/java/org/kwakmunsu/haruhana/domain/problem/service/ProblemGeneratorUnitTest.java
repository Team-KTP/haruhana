package org.kwakmunsu.haruhana.domain.problem.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.UnitTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryTopicFixture;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.DailyProblemManager;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.member.service.MemberReader;
import org.kwakmunsu.haruhana.domain.problem.ProblemFixture;
import org.kwakmunsu.haruhana.domain.problem.entity.Problem;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.domain.problem.repository.ProblemJpaRepository;
import org.kwakmunsu.haruhana.domain.problem.service.dto.ProblemResponse;
import org.kwakmunsu.haruhana.global.support.notification.ErrorNotificationSender;
import org.kwakmunsu.haruhana.infrastructure.gemini.ChatService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

class ProblemGeneratorUnitTest extends UnitTestSupport {

    @Mock
    MemberReader memberReader;

    @Mock
    ChatService chatService;

    @Mock
    ProblemJpaRepository problemJpaRepository;

    @Mock
    DailyProblemManager dailyProblemManager;

    @Mock
    ErrorNotificationSender errorNotificationSender;

    @Mock
    CacheManager cacheManager;

    @InjectMocks
    ProblemGenerator problemGenerator;


    @Test
    void 활성화된_회원_학습_정보가_없으면_문제를_생성하지_않는다() {
        // given
        LocalDate targetDate = LocalDate.now();
        given(memberReader.getMemberPreferences(targetDate)).willReturn(List.of());

        // when
        problemGenerator.generateProblem(targetDate);

        // then
        verify(chatService, never()).sendPrompt(any(), any());
        verify(problemJpaRepository, never()).save(any());
        verify(dailyProblemManager, never()).assignDailyProblemToMembers(any(), any(), any());
    }

    @Test
    void 카테고리와_난이도가_같은_회원들을_그룹화하여_문제를_생성한다() {
        // given
        var targetDate = LocalDate.now();

        var member1 = MemberFixture.createMember(Role.ROLE_MEMBER);
        var member2 = createMemberWithId(2L);
        var member3 = createMemberWithId(3L);

        var javaTopic = CategoryTopicFixture.createCategoryTopic();
        var springTopic = CategoryTopicFixture.createCategoryTopic(2L, "Spring");

        // Java - MEDIUM 그룹 (2명)
        var pref1 = createPreference(member1, javaTopic, ProblemDifficulty.MEDIUM, targetDate);
        var pref2 = createPreference(member2, javaTopic, ProblemDifficulty.MEDIUM, targetDate);

        // Spring - EASY 그룹 (1명)
        var pref3 = createPreference(member3, springTopic, ProblemDifficulty.EASY, targetDate);

        given(memberReader.getMemberPreferences(targetDate))
                .willReturn(List.of(pref1, pref2, pref3));

        ProblemResponse mockResponse = new ProblemResponse(
                "Java의 equals와 hashCode의 관계",
                "Java에서 equals()와 hashCode()를 함께 재정의해야 하는 이유는 무엇인가요?",
                "equals()와 hashCode()는 객체의 동등성 비교에 사용되는 메서드입니다..."
        );

        given(chatService.sendPrompt(any(), any())).willReturn(mockResponse);

        var savedProblem = ProblemFixture.createProblem(1L, javaTopic);
        given(problemJpaRepository.save(any(Problem.class))).willReturn(savedProblem);

        // when
        problemGenerator.generateProblem(targetDate);

        // then
        // 2개의 그룹이므로 2번 문제 생성
        verify(chatService, times(2)).sendPrompt(any(), any());
        verify(problemJpaRepository, times(2)).save(any(Problem.class));
        verify(dailyProblemManager, times(2)).assignDailyProblemToMembers(any(), any(), any());
    }

    @Test
    void 문제_생성_중_예외가_발생해도_다른_그룹의_문제는_계속_생성된다() {
        // given
        var targetDate = LocalDate.now();

        var member1 = MemberFixture.createMember(Role.ROLE_MEMBER);
        var member2 = createMemberWithId(2L);

        var javaTopic = CategoryTopicFixture.createCategoryTopic();
        var springTopic = CategoryTopicFixture.createCategoryTopic(2L, "Spring");

        var pref1 = createPreference(member1, javaTopic, ProblemDifficulty.MEDIUM, targetDate);
        var pref2 = createPreference(member2, springTopic, ProblemDifficulty.EASY, targetDate);

        given(memberReader.getMemberPreferences(targetDate))
                .willReturn(List.of(pref1, pref2));

        ProblemResponse mockResponse = new ProblemResponse(
                "Java의 equals와 hashCode의 관계",
                "Java에서 equals()와 hashCode()를 함께 재정의해야 하는 이유는 무엇인가요?",
                "equals()와 hashCode()는 객체의 동등성 비교에 사용되는 메서드입니다..."
        );

        // 첫 번째 호출은 예외, 두 번째 호출은 성공
        given(chatService.sendPrompt(any(), any()))
                .willThrow(new RuntimeException("AI 서비스 오류"))
                .willReturn(mockResponse);

        var savedProblem = ProblemFixture.createProblem(2L, springTopic);
        given(problemJpaRepository.save(any(Problem.class))).willReturn(savedProblem);

        // when
        problemGenerator.generateProblem(targetDate);

        // then
        // 첫 번째는 실패했지만 두 번째는 성공
        verify(chatService, times(2)).sendPrompt(any(), any());
        verify(problemJpaRepository, times(1)).save(any(Problem.class));
        verify(dailyProblemManager, times(1)).assignDailyProblemToMembers(any(), any(), any());
    }

    @Test
    void 같은_회원이_여러_카테고리를_설정한_경우_각각_문제가_생성된다() {
        // given
        var targetDate = LocalDate.now();

        var member = MemberFixture.createMember(Role.ROLE_MEMBER);

        var javaTopic = CategoryTopicFixture.createCategoryTopic();
        var pythonTopic = CategoryTopicFixture.createCategoryTopic(2L, "Python");

        // 같은 회원이 여러 카테고리 설정
        var pref1 = createPreference(member, javaTopic, ProblemDifficulty.MEDIUM, targetDate);
        var pref2 = createPreference(member, pythonTopic, ProblemDifficulty.EASY, targetDate);

        given(memberReader.getMemberPreferences(targetDate))
                .willReturn(List.of(pref1, pref2));

        ProblemResponse mockResponse = new ProblemResponse(
                "Java의 equals와 hashCode의 관계",
                "Java에서 equals()와 hashCode()를 함께 재정의해야 하는 이유는 무엇인가요?",
                "equals()와 hashCode()는 객체의 동등성 비교에 사용되는 메서드입니다..."
        );

        given(chatService.sendPrompt(any(), any())).willReturn(mockResponse);

        var savedProblem = ProblemFixture.createProblem(1L, javaTopic);
        given(problemJpaRepository.save(any(Problem.class))).willReturn(savedProblem);

        // when
        problemGenerator.generateProblem(targetDate);

        // then
        // 2개의 카테고리이므로 2번 문제 생성
        verify(chatService, times(2)).sendPrompt(any(), any());
        verify(problemJpaRepository, times(2)).save(any(Problem.class));
        verify(dailyProblemManager, times(2)).assignDailyProblemToMembers(any(), any(), any());
    }

    @Test
    void 문제_생성과_백업_할당_모두_실패하면_슬랙_알림이_전송된다() {
        // given
        var targetDate = LocalDate.now();
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var javaTopic = CategoryTopicFixture.createCategoryTopic();
        var pref = createPreference(member, javaTopic, ProblemDifficulty.MEDIUM, targetDate);

        given(memberReader.getMemberPreferences(targetDate)).willReturn(List.of(pref));
        given(chatService.sendPrompt(any(), any())).willThrow(new RuntimeException("AI 서비스 오류"));
        given(problemJpaRepository.findLeastRecentlyAssignedProblem(any(), any(), any()))
                .willThrow(new RuntimeException("백업 문제 조회 실패"));

        // when
        problemGenerator.generateProblem(targetDate);

        // then
        verify(errorNotificationSender, times(1)).sendErrorNotification(any(String.class), any(Exception.class));
    }

    @Test
    void 문제_생성_실패_후_백업이_성공하면_슬랙_알림이_전송되지_않는다() {
        // given
        var targetDate = LocalDate.now();
        var member = MemberFixture.createMember(Role.ROLE_MEMBER);
        var javaTopic = CategoryTopicFixture.createCategoryTopic();
        var pref = createPreference(member, javaTopic, ProblemDifficulty.MEDIUM, targetDate);
        var backupProblem = ProblemFixture.createProblem(1L, javaTopic);

        given(memberReader.getMemberPreferences(targetDate)).willReturn(List.of(pref));
        given(chatService.sendPrompt(any(), any())).willThrow(new RuntimeException("AI 서비스 오류"));
        given(problemJpaRepository.findLeastRecentlyAssignedProblem(any(), any(), any()))
                .willReturn(Optional.of(backupProblem));

        // when
        problemGenerator.generateProblem(targetDate);

        // then
        verify(errorNotificationSender, never()).sendErrorNotification(any(), any());
    }

    private Member createMemberWithId(Long id) {
        var member = MemberFixture.createMemberWithOutId(Role.ROLE_MEMBER);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private MemberPreference createPreference(
            Member member,
            org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic categoryTopic,
            ProblemDifficulty difficulty,
            LocalDate effectiveAt
    ) {
        return MemberPreference.create(member, categoryTopic, difficulty, effectiveAt);
    }


}