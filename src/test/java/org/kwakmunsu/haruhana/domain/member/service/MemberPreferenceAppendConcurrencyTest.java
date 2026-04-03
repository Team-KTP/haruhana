package org.kwakmunsu.haruhana.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.IntegrationTestSupport;
import org.kwakmunsu.haruhana.domain.category.CategoryFactory;
import org.kwakmunsu.haruhana.domain.category.repository.CategoryTopicJpaRepository;
import org.kwakmunsu.haruhana.domain.member.MemberFixture;
import org.kwakmunsu.haruhana.domain.member.repository.MemberJpaRepository;
import org.kwakmunsu.haruhana.domain.member.repository.MemberPreferenceJpaRepository;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.NewPreference;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.domain.problem.service.ProblemGenerator;
import org.kwakmunsu.haruhana.domain.streak.service.StreakManager;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.kwakmunsu.haruhana.global.support.image.StorageProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@RequiredArgsConstructor
class MemberPreferenceAppendConcurrencyTest extends IntegrationTestSupport {

    final CategoryFactory categoryFactory;
    final MemberService memberService;
    final MemberJpaRepository memberJpaRepository;
    final MemberPreferenceJpaRepository memberPreferenceJpaRepository;
    final CategoryTopicJpaRepository categoryTopicJpaRepository;

    @MockitoBean
    ProblemGenerator problemGenerator;

    @MockitoBean
    StreakManager streakManager;

    @MockitoBean
    StorageProvider storageProvider;

    @BeforeEach
    void setUp() {
        categoryFactory.deleteAll();
        categoryFactory.saveAll();

        doNothing().when(problemGenerator).generateInitialProblem(any(), any(), any());
        doNothing().when(streakManager).create(any());
    }

    @AfterEach
    void tearDown() {
        memberPreferenceJpaRepository.deleteAll();
        memberJpaRepository.deleteAll();
        categoryFactory.deleteAll();
    }

    @Test
    void 동시에_5개_선호_정보_추가_요청_시_최대_개수를_초과하지_않는다() throws InterruptedException {
        // given
        var loginId = UUID.randomUUID().toString().substring(0, 20);
        var nickname = UUID.randomUUID().toString().substring(0, 20);
        var member = MemberFixture.createMemberWithOutId(loginId, nickname);
        memberJpaRepository.save(member);

        var javaTopic = categoryTopicJpaRepository.findByName("Java")
                .orElseThrow(() -> new RuntimeException("Java 토픽이 존재하지 않습니다"));
        var pythonTopic = categoryTopicJpaRepository.findByName("Python")
                .orElseThrow(() -> new RuntimeException("Python 토픽이 존재하지 않습니다"));
        var cppTopic = categoryTopicJpaRepository.findByName("C/C++")
                .orElseThrow(() -> new RuntimeException("C/C++ 토픽이 존재하지 않습니다"));
        var jsTopic = categoryTopicJpaRepository.findByName("JavaScript")
                .orElseThrow(() -> new RuntimeException("JavaScript 토픽이 존재하지 않습니다"));

        memberService.appendPreference(new NewPreference(javaTopic.getId(), ProblemDifficulty.MEDIUM), member.getId());
        memberService.appendPreference(new NewPreference(pythonTopic.getId(), ProblemDifficulty.MEDIUM), member.getId());
        memberService.appendPreference(new NewPreference(cppTopic.getId(), ProblemDifficulty.MEDIUM), member.getId());
        memberService.appendPreference(new NewPreference(jsTopic.getId(), ProblemDifficulty.MEDIUM), member.getId());

        var goTopic = categoryTopicJpaRepository.findByName("Go")
                .orElseThrow(() -> new RuntimeException("Go 토픽이 존재하지 않습니다"));
        var kotlinTopic = categoryTopicJpaRepository.findByName("Kotlin")
                .orElseThrow(() -> new RuntimeException("Kotlin 토픽이 존재하지 않습니다"));
        var swiftTopic = categoryTopicJpaRepository.findByName("Swift")
                .orElseThrow(() -> new RuntimeException("Swift 토픽이 존재하지 않습니다"));
        var rubyTopic = categoryTopicJpaRepository.findByName("Ruby")
                .orElseThrow(() -> new RuntimeException("Ruby 토픽이 존재하지 않습니다"));
        var springTopic = categoryTopicJpaRepository.findByName("Spring")
                .orElseThrow(() -> new RuntimeException("Spring 토픽이 존재하지 않습니다"));

        var concurrentTopicIds = new Long[]{
                goTopic.getId(),
                kotlinTopic.getId(),
                swiftTopic.getId(),
                rubyTopic.getId(),
                springTopic.getId()
        };

        int threadCount = 5;
        var executorService = Executors.newFixedThreadPool(threadCount);
        var latch = new CountDownLatch(threadCount);
        var successCount = new AtomicInteger(0);
        var failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    memberService.appendPreference(
                            new NewPreference(concurrentTopicIds[index], ProblemDifficulty.MEDIUM),
                            member.getId()
                    );
                    successCount.incrementAndGet();
                } catch (HaruHanaException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        // then
        var finalCount = memberPreferenceJpaRepository.countByMemberIdAndStatus(member.getId(), EntityStatus.ACTIVE);
        assertThat(finalCount).isEqualTo(5);
        assertThat(successCount.get()).isEqualTo(1);
    }

}
