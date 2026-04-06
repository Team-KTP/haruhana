package org.kwakmunsu.haruhana.domain.dailyproblem.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.IntegrationTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryFactory;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryTopicJpaRepository;
import org.kwakmunsu.haruhana.domain.dailyproblem.entity.DailyProblem;
import org.kwakmunsu.haruhana.domain.dailyproblem.repository.DailyProblemJpaRepository;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.repository.MemberJpaRepository;
import org.kwakmunsu.haruhana.domain.problem.ProblemFixture;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.domain.problem.repository.ProblemJpaRepository;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
class DailyProblemReaderIntegrationTest extends IntegrationTestSupport {

    final CategoryFactory categoryFactory;
    final CategoryTopicJpaRepository categoryTopicJpaRepository;
    final MemberJpaRepository memberJpaRepository;
    final ProblemJpaRepository problemJpaRepository;
    final DailyProblemJpaRepository dailyProblemJpaRepository;
    final DailyProblemReader dailyProblemReader;

    CategoryTopic categoryTopic;

    @BeforeEach
    void setUp() {
        categoryFactory.deleteAll();
        categoryFactory.saveAll();

        categoryTopic = categoryTopicJpaRepository.findByName("Java").orElseThrow();
    }

    @Test
    void 해당날짜에_회원에게_할당된_데일리_문제를_조회한다() {
        // given
        var member = MemberFixture.createMemberWithOutId("loginId", "nickname");
        memberJpaRepository.save(member);

        var problem = ProblemFixture.createProblem("title", "description", categoryTopic, ProblemDifficulty.MEDIUM);
        problemJpaRepository.save(problem);

        var dailyProblem = DailyProblem.create(member, problem, problem.getProblemAt());
        dailyProblemJpaRepository.save(dailyProblem);

        // when
        var result = dailyProblemReader.findDailyProblems(dailyProblem.getAssignedAt(), member.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(dailyProblem.getId());
    }

    @Test
    void 해당날짜에_회원에게_여러_카테고리_문제가_할당된_경우_모두_조회된다() {
        // given
        var member = MemberFixture.createMemberWithOutId("loginId", "nickname");
        memberJpaRepository.save(member);

        var springTopic = categoryTopicJpaRepository.findByName("Spring").orElseThrow();

        var javaProblem = ProblemFixture.createProblem("Java 문제", "Java 설명", categoryTopic, ProblemDifficulty.MEDIUM);
        var springProblem = ProblemFixture.createProblem("Spring 문제", "Spring 설명", springTopic, ProblemDifficulty.EASY);
        problemJpaRepository.save(javaProblem);
        problemJpaRepository.save(springProblem);

        var javaDailyProblem = DailyProblem.create(member, javaProblem, LocalDate.now());
        var springDailyProblem = DailyProblem.create(member, springProblem, LocalDate.now());
        dailyProblemJpaRepository.save(javaDailyProblem);
        dailyProblemJpaRepository.save(springDailyProblem);

        // when
        var result = dailyProblemReader.findDailyProblems(LocalDate.now(), member.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(dp -> dp.getProblem().getCategoryTopic().getName())
                .containsExactlyInAnyOrder("Java", "Spring");
    }

    @Test
    void 해당날짜에_회원에게_할당된_데일리_문제가_존재하지_않는다() {
        // given
        var member = MemberFixture.createMemberWithOutId("loginId", "nickname");
        memberJpaRepository.save(member);

        var problem = ProblemFixture.createProblem("title", "description", categoryTopic, ProblemDifficulty.MEDIUM);
        problemJpaRepository.save(problem);

        var dailyProblem = DailyProblem.create(member, problem, problem.getProblemAt());
        dailyProblemJpaRepository.save(dailyProblem);

        // when
        LocalDate notExistsDailyProblemDate = LocalDate.now().plusDays(4);
        var result = dailyProblemReader.findDailyProblems(notExistsDailyProblemDate, member.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 해당날짜에_문제는_있지만_다른_회원의_문제일_경우_조회되지_않는다() {
        // given
        var member = MemberFixture.createMemberWithOutId("loginId", "nickname");
        memberJpaRepository.save(member);

        var problem = ProblemFixture.createProblem("title", "description", categoryTopic, ProblemDifficulty.MEDIUM);
        problemJpaRepository.save(problem);

        var dailyProblem = DailyProblem.create(member, problem, problem.getProblemAt());
        dailyProblemJpaRepository.save(dailyProblem);

        // when
        Long anotherMemberId = member.getId() + 1;
        var result = dailyProblemReader.findDailyProblems(dailyProblem.getAssignedAt(), anotherMemberId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void assignedAt이_NULL일_경우_현재_날짜_기준으로_조회한다() {
        // given
        var member = MemberFixture.createMemberWithOutId("loginId", "nickname");
        memberJpaRepository.save(member);

        var problem = ProblemFixture.createProblem("title", "description", categoryTopic, ProblemDifficulty.MEDIUM);
        problemJpaRepository.save(problem);

        var dailyProblem = DailyProblem.create(member, problem, problem.getProblemAt());
        dailyProblemJpaRepository.save(dailyProblem);

        // when
        var result = dailyProblemReader.findDailyProblems(null, member.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(dailyProblem.getId());
    }

}