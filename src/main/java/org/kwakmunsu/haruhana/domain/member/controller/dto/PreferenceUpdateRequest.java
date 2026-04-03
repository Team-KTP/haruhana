package org.kwakmunsu.haruhana.domain.member.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.kwakmunsu.haruhana.domain.member.service.dto.request.UpdatePreference;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;

@Schema(description = "회원 학습 설정 수정 요청 DTO")
public record PreferenceUpdateRequest(
        @Schema(description = "카테고리 ID", example = "1")
        @NotNull(message = "카테고리는 필수입니다.")
        Long categoryTopicId,

        @Schema(description = "문제 난이도", example = "EASY")
        @NotNull(message = "난이도는 필수입니다.")
        ProblemDifficulty difficulty
) {

    public UpdatePreference toUpdatePreference(Long preferenceId) {
        return UpdatePreference.builder()
                .preferenceId(preferenceId)
                .categoryTopicId(categoryTopicId)
                .difficulty(difficulty)
                .build();
    }

}