package org.kwakmunsu.haruhana.domain.submission.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kwakmunsu.haruhana.domain.submission.service.FeedbackGradingService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class FeedbackEventHandler {

    private final FeedbackGradingService feedbackGradingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubmissionCompleted(SubmissionCompletedEvent event) {
        log.info("[FeedbackEventHandler] AI 채점 시작 - submissionId: {}", event.submissionId());
        feedbackGradingService.grade(event.submissionId());
    }

}