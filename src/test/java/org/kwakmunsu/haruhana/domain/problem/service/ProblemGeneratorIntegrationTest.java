package org.kwakmunsu.haruhana.domain.problem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.IntegrationTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryFactory;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryTopicJpaRepository;
import org.kwakmunsu.haruhana.domain.dailyproblem.entity.DailyProblem;
import org.kwakmunsu.haruhana.domain.dailyproblem.repository.DailyProblemJpaRepository;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;
import org.kwakmunsu.haruhana.domain.member.enums.Role;
import org.kwakmunsu.haruhana.domain.member.repository.MemberJpaRepository;
import org.kwakmunsu.haruhana.domain.member.repository.MemberPreferenceJpaRepository;
import org.kwakmunsu.haruhana.domain.problem.entity.Problem;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.domain.problem.repository.ProblemJpaRepository;
import org.kwakmunsu.haruhana.domain.problem.service.dto.ProblemResponse;
import org.kwakmunsu.haruhana.infrastructure.gemini.ChatService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
class ProblemGeneratorIntegrationTest extends IntegrationTestSupport {

    final ProblemGenerator problemGenerator;
    final CategoryFactory categoryFactory;
    final CategoryTopicJpaRepository categoryTopicJpaRepository;
    final MemberJpaRepository memberJpaRepository;
    final MemberPreferenceJpaRepository memberPreferenceJpaRepository;
    final ProblemJpaRepository problemJpaRepository;
    final DailyProblemJpaRepository dailyProblemJpaRepository;
    final EntityManager entityManager;

    @MockitoBean
    ChatService chatService;

    @BeforeEach
    void setUp() {
        categoryFactory.deleteAll();
        categoryFactory.saveAll();
        entityManager.flush();

        // ChatService Mock 응답 설정
        String mockJsonResponse = """
                {
                    "title": "테스트 문제 제목",
                    "description": "테스트 문제 설명입니다.",
                    "aiAnswer": "테스트 모범 답변입니다."
                }
                """;
        given(chatService.sendPrompt(any(), any())).willReturn(mockJsonResponse);
    }

    @Test
    void 활성화된_회원_학습_정보가_없으면_문제를_생성하지_않는다() {
        // given
        var targetDate = LocalDate.now();

        // when
        problemGenerator.generateProblem(targetDate);
        entityManager.flush();

        // then
        assertThat(problemJpaRepository.findAll()).isEmpty();
        assertThat(dailyProblemJpaRepository.findAll()).isEmpty();
    }

    @Test
    void 카테고리와_난이도별로_문제를_생성하고_회원에게_할당한다() {
        // given
        var targetDate = LocalDate.now();

        var javaTopic = categoryTopicJpaRepository.findByName("Java")
                .orElseThrow(() -> new RuntimeException("Java 토픽이 존재하지 않습니다"));

        var member1 = memberJpaRepository.save(Member.createMember("user1", "password123!", "닉네임1", Role.ROLE_MEMBER));
        var member2 = memberJpaRepository.save(Member.createMember("user2", "password123!", "닉네임2", Role.ROLE_MEMBER));

        memberPreferenceJpaRepository.save(MemberPreference.create(member1, javaTopic, ProblemDifficulty.MEDIUM, targetDate));
        memberPreferenceJpaRepository.save(MemberPreference.create(member2, javaTopic, ProblemDifficulty.MEDIUM, targetDate));

        entityManager.flush();
        ProblemResponse mockResponse = new ProblemResponse(
                "Java의 equals와 hashCode의 관계",
                "Java에서 equals()와 hashCode()를 함께 재정의해야 하는 이유는 무엇인가요?",
                "equals()와 hashCode()는 객체의 동등성 비교에 사용되는 메서드입니다..."
        );
        given(chatService.sendPrompt(any(), any())).willReturn(mockResponse);

        // when
        problemGenerator.generateProblem(targetDate);
        entityManager.flush();
        entityManager.clear();

        // then
        var problems = problemJpaRepository.findAll();
        assertThat(problems).hasSize(1);

        var problem = problems.getFirst();

        assertThat(problem.getTitle()).isNotEmpty();
        assertThat(problem.getDescription()).isNotEmpty();
        assertThat(problem.getAiAnswer()).isNotEmpty();
        assertThat(problem.getCategoryTopic().getName()).isEqualTo("Java");
        assertThat(problem.getDifficulty()).isEqualTo(ProblemDifficulty.MEDIUM);
        assertThat(problem.getProblemAt()).isEqualTo(targetDate);

        var dailyProblems = dailyProblemJpaRepository.findAll();
        assertThat(dailyProblems).hasSize(2);
        assertThat(dailyProblems)
                .extracting(DailyProblem::getProblem)
                .allMatch(p -> p.getId().equals(problem.getId()));
    }

    // [참고] @MockitoBean은 Spring AOP 프록시를 우회하므로 ChatService의 @Retryable이 적용되지 않습니다.
    // 백업 테스트는 AI 호출 최종 실패 이후의 폴백 동작만 검증하며, 재시도 횟수 자체는 검증하지 않습니다.
    @Test
    void AI_호출_실패_시_백업_문제가_존재하면_기존_문제가_할당된다() {
        // given
        var targetDate = LocalDate.now();
        var javaTopic = categoryTopicJpaRepository.findByName("Java")
                .orElseThrow(() -> new RuntimeException("Java 토픽이 존재하지 않습니다"));

        var member = memberJpaRepository.save(Member.createMember("user_backup", "password123!", "백업회원", Role.ROLE_MEMBER));
        memberPreferenceJpaRepository.save(MemberPreference.create(member, javaTopic, ProblemDifficulty.MEDIUM, targetDate));

        Problem backupProblem = problemJpaRepository.save(Problem.create(
                "백업 문제 제목",
                "백업 문제 설명",
                "백업 AI 답변",
                javaTopic,
                ProblemDifficulty.MEDIUM,
                targetDate.minusDays(1),
                Prompt.V1_PROMPT.name()
        ));
        entityManager.flush();

        // setUp의 stub을 예외를 던지도록 덮어씀
        given(chatService.sendPrompt(any(), any())).willThrow(new RuntimeException("AI API 오류"));

        // when
        problemGenerator.generateProblem(targetDate);
        entityManager.flush();
        entityManager.clear();

        // then - 새 문제는 생성되지 않고 기존 백업 문제가 DailyProblem으로 할당됨
        assertThat(problemJpaRepository.count()).isEqualTo(1);
        assertThat(dailyProblemJpaRepository.count()).isEqualTo(1);
        assertThat(dailyProblemJpaRepository.findAll().getFirst().getProblem().getId())
                .isEqualTo(backupProblem.getId());
    }

    @Test
    void AI_호출_실패_시_백업_문제가_없으면_할당_없이_정상_종료된다() {
        // given
        var targetDate = LocalDate.now();
        var javaTopic = categoryTopicJpaRepository.findByName("Java")
                .orElseThrow(() -> new RuntimeException("Java 토픽이 존재하지 않습니다"));

        var member = memberJpaRepository.save(Member.createMember("user_no_backup", "password123!", "백업없는회원", Role.ROLE_MEMBER));
        memberPreferenceJpaRepository.save(MemberPreference.create(member, javaTopic, ProblemDifficulty.MEDIUM, targetDate));
        entityManager.flush();

        // setUp의 stub을 예외를 던지도록 덮어씀
        given(chatService.sendPrompt(any(), any())).willThrow(new RuntimeException("AI API 오류"));

        // when - 예외 없이 정상 종료되어야 함
        problemGenerator.generateProblem(targetDate);
        entityManager.flush();

        // then - 문제도 DailyProblem도 생성되지 않음
        assertThat(problemJpaRepository.count()).isZero();
        assertThat(dailyProblemJpaRepository.count()).isZero();
    }

    @Test
    void 카테고리와_난이도가_다르면_각각_다른_문제를_생성한다() {
        // given
        var targetDate = LocalDate.now();

        var javaTopic = categoryTopicJpaRepository.findByName("Java")
                .orElseThrow(() -> new RuntimeException("Java 토픽이 존재하지 않습니다"));
        var springTopic = categoryTopicJpaRepository.findByName("Spring")
                .orElseThrow(() -> new RuntimeException("Spring 토픽이 존재하지 않습니다"));

        var member1 = memberJpaRepository.save(Member.createMember("user1", "password123!", "닉네임1", Role.ROLE_MEMBER));
        var member2 = memberJpaRepository.save(Member.createMember("user2", "password123!", "닉네임2", Role.ROLE_MEMBER));

        // Java - MEDIUM
        memberPreferenceJpaRepository.save(MemberPreference.create(member1, javaTopic, ProblemDifficulty.MEDIUM, targetDate));

        // Spring - EASY
        memberPreferenceJpaRepository.save(MemberPreference.create(member2, springTopic, ProblemDifficulty.EASY, targetDate));

        entityManager.flush();
        ProblemResponse mockResponse = new ProblemResponse(
                "Java의 equals와 hashCode의 관계",
                "Java에서 equals()와 hashCode()를 함께 재정의해야 하는 이유는 무엇인가요?",
                "equals()와 hashCode()는 객체의 동등성 비교에 사용되는 메서드입니다..."
        );
        ProblemResponse springResponse = new ProblemResponse(
                "Spring DI란?",
                "Spring에서 DI란 무엇인가요?",
                "Spring 답변"
        );
        given(chatService.sendPrompt(any(), any()))
                .willReturn(mockResponse)
                .willReturn(springResponse);

        // when
        problemGenerator.generateProblem(targetDate);
        entityManager.flush();
        entityManager.clear();

        // then
        var problems = problemJpaRepository.findAll();
        assertThat(problems).hasSize(2);

        var dailyProblems = dailyProblemJpaRepository.findAll();
        assertThat(dailyProblems).hasSize(2);
    }


}