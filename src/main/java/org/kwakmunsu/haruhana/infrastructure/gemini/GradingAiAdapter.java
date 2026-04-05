package org.kwakmunsu.haruhana.infrastructure.gemini;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kwakmunsu.haruhana.domain.problem.service.Prompt;
import org.kwakmunsu.haruhana.domain.submission.service.dto.FeedbackGradingResult;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class GradingAiAdapter {

    private final ChatService chatService;  // app.ai.provider에 따라 Primary 빈이 주입됨

    /**
     * AI 채점 수행 (재시도 로직 포함)
     * - 외부 AI API 호출의 복원력을 위해 지수 백오프로 최대 3회 재시도
     * - 실패 시 @Recover로 명확한 에러 로깅
     *
     * @param problemDescription 문제 설명
     * @param aiAnswer AI 모범 답안
     * @param userAnswer 사용자 답변
     * @return 채점 결과
     * @throws HaruHanaException 3회 재시도 후에도 실패
     */
    @Retryable(
            retryFor = {HaruHanaException.class},
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000
            )
    )
    public FeedbackGradingResult grade(String problemDescription, String aiAnswer, String userAnswer) {
        String prompt = Prompt.GRADING_PROMPT.generateGrading(problemDescription, aiAnswer, userAnswer);
        try {
            FeedbackGradingResult result = chatService.sendPrompt(prompt, FeedbackGradingResult.class);
            result.validate();
            return result;
        } catch (Exception e) {
            log.error("[GradingAiAdapter] AI 채점 실패 - problemDescription={}", problemDescription, e);
            throw new HaruHanaException(ErrorType.FAIL_TO_GRADE_SUBMISSION);
        }
    }

    /**
     * AI 호출 재시도 3회 모두 실패 시 호출되는 복구 메서드
     * - 최종 실패를 명확하게 로깅
     *
     * @param exception 마지막 시도의 예외
     * @param problemDescription 문제 설명
     * @param aiAnswer AI 모범 답안
     * @param userAnswer 사용자 답변
     * @throws HaruHanaException 최종 실패 예외
     */
    @Recover
    public FeedbackGradingResult recoverFromFailure(Exception exception, String problemDescription, String aiAnswer, String userAnswer) {
        log.error("[GradingAiAdapter] AI 채점 재시도 3회 모두 실패 - problemDescription={}", problemDescription, exception);
        throw new HaruHanaException(ErrorType.FAIL_TO_GRADE_SUBMISSION);
    }

}
