package org.kwakmunsu.haruhana.domain.submission.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kwakmunsu.haruhana.domain.problem.entity.Problem;
import org.kwakmunsu.haruhana.domain.submission.entity.Submission;
import org.kwakmunsu.haruhana.domain.submission.service.dto.FeedbackGradingResult;
import org.kwakmunsu.haruhana.domain.submission.service.dto.response.FeedbackResponse;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.error.HaruHanaException;
import org.kwakmunsu.haruhana.infrastructure.gemini.GradingAiAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class FeedbackGradingService {

    private final SubmissionReader submissionReader;
    private final FeedbackManager feedbackManager;
    private final FeedbackReader feedbackReader;
    private final GradingAiAdapter gradingAiAdapter;

    /**
     * AI 채점 수행 및 결과 저장
     * - Submission 조회 (DailyProblem + Problem fetch join) <br>
     * - 채점 프롬프트 생성 후 AI 호출 (GradingAiAdapter에서 재시도 처리) <br>
     * - 채점 결과 저장 위임 (FeedbackManager)
     *
     * @param submissionId 채점 대상 제출 ID
     */
    public void grade(Long submissionId) {
        Submission submission = submissionReader.findWithProblem(submissionId);
        Problem problem = submission.getDailyProblem().getProblem();

        FeedbackGradingResult result = gradingAiAdapter.grade(
                problem.getDescription(),
                problem.getAiAnswer(),
                submission.getAnswer()
        );

        feedbackManager.save(submission, result);

        log.info("[FeedbackGradingService] 채점 완료 - submissionId: {}, grade: {}", submissionId, result.grade());
    }

    /**
     * 특정 제출의 채점 이력 조회 (최신순)
     *
     * @param submissionId 제출 ID
     * @param memberId     요청 회원 ID (소유권 검증)
     */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbacks(Long submissionId, Long memberId) {
        Submission submission = submissionReader.findById(submissionId);

        if (!submission.isAuthor(memberId)) {
            throw new HaruHanaException(ErrorType.FORBIDDEN_ERROR);
        }

        return feedbackReader.findAllBySubmissionId(submissionId)
                .stream()
                .map(FeedbackResponse::from)
                .toList();
    }

}