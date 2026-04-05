package org.kwakmunsu.haruhana.domain.submission.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.submission.service.FeedbackGradingService;
import org.kwakmunsu.haruhana.domain.submission.service.dto.response.FeedbackResponse;
import org.kwakmunsu.haruhana.global.annotation.LoginMember;
import org.kwakmunsu.haruhana.global.support.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class SubmissionController extends SubmissionDocsController {

    private final FeedbackGradingService feedbackGradingService;

    @Override
    @GetMapping("/v1/submissions/{submissionId}/feedbacks")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getFeedbacks(
            @PathVariable Long submissionId,
            @LoginMember Long memberId
    ) {
        List<FeedbackResponse> response = feedbackGradingService.getFeedbacks(submissionId, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}