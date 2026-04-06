package org.kwakmunsu.haruhana.domain.problem.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kwakmunsu.haruhana.domain.category.entity.CategoryTopic;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.DailyProblemManager;
import org.kwakmunsu.haruhana.domain.member.entity.Member;
import org.kwakmunsu.haruhana.domain.member.entity.MemberPreference;
import org.kwakmunsu.haruhana.domain.member.service.MemberReader;
import org.kwakmunsu.haruhana.domain.problem.entity.Problem;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.domain.problem.repository.ProblemJpaRepository;
import org.kwakmunsu.haruhana.domain.problem.service.dto.ProblemGenerationGroup;
import org.kwakmunsu.haruhana.domain.problem.service.dto.ProblemGenerationKey;
import org.kwakmunsu.haruhana.domain.problem.service.dto.ProblemResponse;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.kwakmunsu.haruhana.global.support.notification.ErrorNotificationSender;
import org.kwakmunsu.haruhana.infrastructure.gemini.ChatService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProblemGenerator {

    private static final int RECENT_DAYS_LIMIT = 30;

    private static final Prompt DEFAULT_PROMPT = Prompt.V3_PROMPT;

    private final MemberReader memberReader;
    private final ChatService chatService;
    private final ProblemJpaRepository problemJpaRepository;
    private final DailyProblemManager dailyProblemManager;
    private final ErrorNotificationSender errorNotificationSender;

    @Transactional
    public void generateProblem(LocalDate targetDate) {
        // 23: 55 실행 되고 익일 날짜 포함해서 그 전날까지 활성화 되어있는 회원 Preference 조회
        List<MemberPreference> memberPreferences = memberReader.getMemberPreferences(targetDate);

        if (memberPreferences.isEmpty()) {
            log.info("[ProblemGenerator] 생성할 문제가 없습니다. 대상 날짜: {}", targetDate);
            return;
        }

        // 카테고리, 난이도 별로 문제를 생성한다
        List<ProblemGenerationGroup> generationGroups = buildGenerationGroups(memberPreferences);

        for (ProblemGenerationGroup group : generationGroups) {
            try {
                Problem problem = generateAndSaveProblem(group, targetDate);
                dailyProblemManager.assignDailyProblemToMembers(problem, group.members(), targetDate);
            } catch (Exception e) {
                log.error("[ProblemGenerator] 문제 생성 실패 - 카테고리: {}, 난이도: {}", group.key().categoryTopicName(),
                        group.key().difficulty(), e);

                try {
                    assignBackupProblem(
                            group.key().categoryTopicId(),
                            group.key().categoryTopicName(),
                            group.key().difficulty(),
                            group.members(),
                            targetDate
                    );
                } catch (Exception backupEx) {
                    log.error("[ProblemGenerator] 백업 문제 할당도 실패 - 카테고리: {}, 난이도: {}", group.key().categoryTopicName(),
                            group.key().difficulty(), backupEx);
                    String message = String.format(
                            "[ProblemGenerator] 문제 생성 및 백업 할당 모두 실패 - 카테고리: %s, 난이도: %s",
                            group.key().categoryTopicName(), group.key().difficulty()
                    );
                    RuntimeException notificationEx = new RuntimeException(message, backupEx);
                    notificationEx.addSuppressed(e); // 1차 실패 원인 보존
                    errorNotificationSender.sendErrorNotification(message, notificationEx);
                }
            }
        }
    }

    /**
     * 회원의 첫 문제를 생성하고 할당
     *
     * @param member        회원
     * @param categoryTopic 카테고리 주제
     * @param difficulty    난이도
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(cacheNames = "todayProblem", key = "#memberId + ':' + T(java.time.LocalDate).now()")
    public void generateInitialProblem(Member member, CategoryTopic categoryTopic, ProblemDifficulty difficulty) {
        LocalDate today = LocalDate.now();
        try {
            ProblemResponse problemResponse = getProblemToAi(categoryTopic.getName(), difficulty);
            validateProblemResponse(problemResponse);

            Problem problem = problemJpaRepository.save(Problem.create(
                    problemResponse.title(),
                    problemResponse.description(),
                    problemResponse.aiAnswer(),
                    categoryTopic,
                    difficulty,
                    today,
                    DEFAULT_PROMPT.name()
            ));
            // 기존 메서드 사용 할려고 그냥 List로 감싸서 보냄
            dailyProblemManager.assignDailyProblemToMembers(problem, List.of(member), today);

            log.info("[ProblemGenerator] 첫 문제 생성 완료 - 카테고리: {}, 난이도: {}, 대상 회원: {}",
                    categoryTopic.getName(),
                    difficulty,
                    member.getId()
            );
        } catch (Exception e) {
            log.error("[ProblemGenerator] 문제 생성 실패 - 카테고리: {}, 난이도: {}", categoryTopic.getName(), difficulty, e);

            assignBackupProblem(categoryTopic.getId(), categoryTopic.getName(), difficulty, List.of(member), today);
        }
    }

    /**
     * 카테고리 주제와 난이도별로 그룹화하여 ProblemGenerationGroup 목록 생성
     */
    private List<ProblemGenerationGroup> buildGenerationGroups(List<MemberPreference> preferences) {
        return preferences.stream()
                .collect(Collectors.groupingBy(preference ->
                        ProblemGenerationKey.of(
                                preference.getCategoryTopic().getId(),
                                preference.getCategoryTopic().getName(),
                                preference.getDifficulty()
                        )
                ))
                .entrySet().stream()
                .map(entry -> ProblemGenerationGroup.builder()
                        .key(entry.getKey())
                        // 첫 번째 Preference 에서 CategoryTopic 가져오기 (모두 동일함)
                        .categoryTopic(entry.getValue().getFirst().getCategoryTopic())
                        .members(entry.getValue().stream()
                                .map(MemberPreference::getMember)
                                .distinct()
                                .toList())
                        .build())
                .toList();
    }

    /**
     * 그룹별로 문제 생성 및 저장
     */
    private Problem generateAndSaveProblem(ProblemGenerationGroup group, LocalDate problemAt) {
        ProblemGenerationKey key = group.key();

        List<String> recentTitles = problemJpaRepository.findRecentTitlesByCategoryTopicIdAndDifficulty(
                key.categoryTopicId(),
                key.difficulty(),
                EntityStatus.ACTIVE,
                problemAt.minusDays(RECENT_DAYS_LIMIT)
        );

        ProblemResponse problemResponse = getProblemToAi(key.categoryTopicName(), key.difficulty(), recentTitles);
        validateProblemResponse(problemResponse);

        Problem saved = problemJpaRepository.save(Problem.create(
                problemResponse.title(),
                problemResponse.description(),
                problemResponse.aiAnswer(),
                group.categoryTopic(),  // 그룹에 포함된 CategoryTopic 사용
                key.difficulty(),
                problemAt,
                DEFAULT_PROMPT.name()
        ));

        log.info("[ProblemGenerator] 문제 생성 완료 - 카테고리: {}, 난이도: {}, 대상 회원 수: {}",
                key.categoryTopicName(),
                key.difficulty(),
                group.getMemberCount()
        );

        return saved;
    }

    private ProblemResponse getProblemToAi(String categoryTopicName, ProblemDifficulty difficulty) {
        String prompt = DEFAULT_PROMPT.generate(categoryTopicName, difficulty);

        return chatService.sendPrompt(prompt, ProblemResponse.class);
    }

    private ProblemResponse getProblemToAi(String categoryTopicName, ProblemDifficulty difficulty, List<String> recentTitles) {
        String prompt = DEFAULT_PROMPT.generate(categoryTopicName, difficulty, recentTitles);

        return chatService.sendPrompt(prompt, ProblemResponse.class);
    }

    private void assignBackupProblem(
            Long categoryTopicId,
            String categoryTopicName,
            ProblemDifficulty difficulty,
            List<Member> members,
            LocalDate targetDate
    ) {
        problemJpaRepository.findLeastRecentlyAssignedProblem(categoryTopicId, difficulty, EntityStatus.ACTIVE)
                .ifPresentOrElse(backup -> {
                            dailyProblemManager.assignDailyProblemToMembers(backup, members, targetDate);
                            log.info("[ProblemGenerator] 백업 문제 할당 완료 - 카테고리: {}, 난이도: {}, 회원 수: {}", categoryTopicName, difficulty,
                                    members.size());
                        },
                        () -> log.warn("[ProblemGenerator] 백업 문제 없음, 할당 생략 - 카테고리: {}, 난이도: {}", categoryTopicName, difficulty)
                );
    }

    private void validateProblemResponse(ProblemResponse problemResponse) {
        if (!problemResponse.isValid()) {
            throw new HaruHanaException(ErrorType.FAIL_TO_GENERATE_PROBLEM);
        }
    }

}