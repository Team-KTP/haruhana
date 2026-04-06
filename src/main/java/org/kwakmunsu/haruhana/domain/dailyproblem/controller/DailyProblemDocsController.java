package org.kwakmunsu.haruhana.domain.dailyproblem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.kwakmunsu.haruhana.domain.dailyproblem.controller.dto.SubmitSolutionRequest;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.DailyProblemDetailResponse;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.DailyProblemResponse;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.dto.response.TodayProblemResponse;
import org.kwakmunsu.haruhana.domain.submission.service.dto.response.SubmissionResponse;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.response.ApiResponse;
import org.kwakmunsu.haruhana.global.swagger.ApiExceptions;
import org.springframework.http.ResponseEntity;

@Tag(name = "DailyProblem Docs", description = "DailyProblem 관련 API 문서")
public abstract class DailyProblemDocsController {

    @Operation(
            summary = "오늘의 문제 조회 - JWT [O]",
            description = """
                    ### 회원의 오늘의 문제를 조회합니다.
                    - 매일 자정에 새로운 문제가 자동으로 할당됩니다.
                    - 회원이 문제를 푼 경우, isSolved 필드가 true로 표시됩니다.
                    """
    )
    @ApiExceptions(values = {
            ErrorType.FORBIDDEN_ERROR,
            ErrorType.NOT_FOUND_DAILY_PROBLEM,
            ErrorType.UNAUTHORIZED_ERROR,
            ErrorType.DEFAULT_ERROR
    })
    public abstract ResponseEntity<ApiResponse<List<TodayProblemResponse>>> getTodayProblem(Long memberId);

    @Operation(
            summary = "날짜에 해당하는 회원에게 할당된 데일리 문제 미리보기 조회 - JWT [O]",
            description = """
                    ### 특정 날짜에 회원에게 할당된 데일리 문제의 미리보기를 조회합니다.
                    - date 파라미터가 제공되지 않은 경우, 오늘 날짜로 간주됩니다.
                    - 해당 날짜에 할당된 문제가 없는 경우, 빈 응답이 반환됩니다.
                    """
    )
    @ApiExceptions(values = {
            ErrorType.FORBIDDEN_ERROR,
            ErrorType.UNAUTHORIZED_ERROR,
            ErrorType.DEFAULT_ERROR
    })
    public abstract ResponseEntity<ApiResponse<List<DailyProblemResponse>>> findDailyProblems(
            LocalDate date,
            Long memberId
    );

    @Operation(
            summary = "문제 상세 조회 - JWT [O]",
            description = """
                    ### 문제 상세 정보를 조회합니다.
                    - 문제의 제목, 설명, 난이도, 카테고리 토픽, 제출 기록 등을 포함합니다.
                    - 회원이 문제를 푼 경우, 제출 기록이 함께 반환됩니다.
                    - 제출 기록이 없는 경우 aiAnswer, submittedAt, userAnswer 필드는 null로 표시됩니다.
                    - 요청한 회원이 해당 문제에 접근할 수 없는 경우, 예외가 발생합니다.
                    - 문제를 찾을 수 없는 경우, 예외가 발생합니다.
                    """
    )
    @ApiExceptions(values = {
            ErrorType.FORBIDDEN_ERROR,
            ErrorType.NOT_FOUND_DAILY_PROBLEM,
            ErrorType.UNAUTHORIZED_ERROR,
            ErrorType.DEFAULT_ERROR
    })
    public abstract ResponseEntity<ApiResponse<DailyProblemDetailResponse>> getDailyProblem(Long dailyProblemId, Long memberId);

    @Operation(
            summary = "문제 제출 - JWT [O]",
            description = """
                    ### 문제에 대한 답변을 제출합니다.
                    - 이미 제출한 경우, 답변이 업데이트됩니다.
                    - 제출 기록이 없는 경우, 새로운 제출을 생성합니다.
                    - 할당 날짜 내에 제출한 경우 isOnTime = true (스트릭 증가 가능)
                    - 할당 날짜 이후에 제출한 경우 isOnTime = false (스트릭 증가 안됨)
                    - 제출 후 AI 모범 답안을 확인할 수 있습니다.
                    - 제출 완료 시 DailyProblem의 isSolved 필드가 true로 변경됩니다.
                    - 제출 성공 시 스트릭 반영
                    """
    )
    @ApiExceptions(values = {
            ErrorType.FORBIDDEN_ERROR,
            ErrorType.NOT_FOUND_DAILY_PROBLEM,
            ErrorType.UNAUTHORIZED_ERROR,
            ErrorType.DEFAULT_ERROR
    })
    public abstract ResponseEntity<ApiResponse<SubmissionResponse>> submitSolution(
            @Parameter(description = "오늘의 문제 ID", example = "1")
            Long dailyProblemId,
            Long memberId,
            SubmitSolutionRequest request
    );

}