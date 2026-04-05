package org.kwakmunsu.haruhana.infrastructure.gemini;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kwakmunsu.haruhana.domain.problem.service.Prompt;
import org.kwakmunsu.haruhana.domain.submission.service.dto.FeedbackGradingResult;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class GradingAiAdapter {

    private final ChatService chatService;  // app.ai.provider에 따라 Primary 빈이 주입됨

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

}
