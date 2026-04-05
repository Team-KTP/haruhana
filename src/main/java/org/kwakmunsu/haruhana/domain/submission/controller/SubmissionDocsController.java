package org.kwakmunsu.haruhana.domain.submission.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.kwakmunsu.haruhana.domain.submission.service.dto.response.FeedbackResponse;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.response.ApiResponse;
import org.kwakmunsu.haruhana.global.swagger.ApiExceptions;
import org.springframework.http.ResponseEntity;

@Tag(name = "Submission Docs", description = "Submission 관련 API 문서")
public abstract class SubmissionDocsController {

    @Operation(
            summary = "AI 채점 이력 조회 - JWT [O]",
            description = """
                    ### 특정 제출에 대한 AI 채점 이력을 최신순으로 조회합니다.
                    - 제출 시 자동으로 AI 채점이 비동기로 진행됩니다.
                    - 채점이 아직 완료되지 않은 경우 빈 배열이 반환됩니다.
                    - 답변 수정 시마다 새로운 채점 결과가 추가되어 이력이 쌓입니다.
                    - 본인의 제출에만 접근 가능합니다.
                    """
    )
    @ApiExceptions(values = {
            ErrorType.FORBIDDEN_ERROR,
            ErrorType.NOT_FOUND_SUBMISSION,
            ErrorType.UNAUTHORIZED_ERROR,
            ErrorType.DEFAULT_ERROR
    })
    public abstract ResponseEntity<ApiResponse<List<FeedbackResponse>>> getFeedbacks(
            @Parameter(description = "제출 ID", example = "1")
            Long submissionId,
            Long memberId
    );

}