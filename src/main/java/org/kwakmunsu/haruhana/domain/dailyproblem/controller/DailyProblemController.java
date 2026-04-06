package org.kwakmunsu.haruhana.domain.dailyproblem.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.dailyproblem.controller.dto.SubmitSolutionRequest;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.DailyProblemService;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.DailyProblemDetailResponse;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.DailyProblemResponse;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.TodayProblemResponse;
import org.kwakmunsu.haruhana.domain.submission.service.SubmissionService;
import org.kwakmunsu.haruhana.domain.submission.service.dto.response.SubmissionResponse;
import org.kwakmunsu.haruhana.global.annotation.LoginMember;
import org.kwakmunsu.haruhana.global.support.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class DailyProblemController extends DailyProblemDocsController {

    private final DailyProblemService dailyProblemService;
    private final SubmissionService submissionService;

    @Override
    @GetMapping("/v1/daily-problem/today")
    public ResponseEntity<ApiResponse<List<TodayProblemResponse>>> getTodayProblem(@LoginMember Long memberId) {
        List<TodayProblemResponse> response = dailyProblemService.getTodayProblem(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @GetMapping("/v1/daily-problem")
    public ResponseEntity<ApiResponse<List<DailyProblemResponse>>> findDailyProblems(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @LoginMember Long memberId
    ) {
        List<DailyProblemResponse> response = dailyProblemService.findDailyProblems(date, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @GetMapping("/v1/daily-problem/{dailyProblemId}")
    public ResponseEntity<ApiResponse<DailyProblemDetailResponse>> getDailyProblem(
            @PathVariable Long dailyProblemId,
            @LoginMember Long memberId
    ) {
        DailyProblemDetailResponse response = dailyProblemService.getDailyProblem(dailyProblemId, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @PostMapping("/v1/daily-problem/{dailyProblemId}/submissions")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitSolution(
            @PathVariable Long dailyProblemId,
            @LoginMember Long memberId,
            @RequestBody @Valid SubmitSolutionRequest request
    ) {
        SubmissionResponse response = submissionService.submitSolution(dailyProblemId, memberId, request.userAnswer());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}