package org.kwakmunsu.haruhana.infrastructure.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.IntegrationTestSupport;
import org.kwakmunsu.haruhana.domain.problem.enums.ProblemDifficulty;
import org.kwakmunsu.haruhana.domain.problem.service.Prompt;
import org.kwakmunsu.haruhana.domain.problem.service.dto.ProblemResponse;

/**
 * 실제 Gemini API를 호출하는 테스트
 * 비용 발생 가능성이 있으므로 기본적으로 비활성화
 */
@RequiredArgsConstructor
class ChatServiceRealApiTest extends IntegrationTestSupport {

    final ChatService chatService;

    @Test
    @Disabled("실제 API 호출 테스트 - 필요할 때만 활성화")
    void Java_중급_난이도_문제를_생성한다() {
        // given
        String prompt = Prompt.V1_PROMPT.generate("Java", ProblemDifficulty.MEDIUM);

        // when
        ProblemResponse problemResponse = chatService.sendPrompt(prompt, ProblemResponse.class);

        // then
        assertThat(problemResponse).isNotNull();
        assertThat(problemResponse.title()).isNotEmpty();
        assertThat(problemResponse.description()).isNotEmpty();
        assertThat(problemResponse.aiAnswer()).isNotEmpty();

        System.out.println("=== Gemini API 응답 ===");
        System.out.println(problemResponse);
    }

    @Test
    @Disabled("실제 API 호출 테스트 - 필요할 때만 활성화")
    void Spring_초급_난이도_문제를_생성한다() {
        // given
        String prompt = Prompt.V1_PROMPT.generate("Spring", ProblemDifficulty.EASY);

        // when
        ProblemResponse problemResponse = chatService.sendPrompt(prompt, ProblemResponse.class);

        // then
        assertThat(problemResponse).isNotNull();
        assertThat(problemResponse.title()).isNotEmpty();
        assertThat(problemResponse.description()).isNotEmpty();
        assertThat(problemResponse.aiAnswer()).isNotEmpty();

        System.out.println("=== Gemini API 응답 ===");
        System.out.println(problemResponse);
    }

    @Test
    @Disabled("실제 API 호출 테스트 - 필요할 때만 활성화")
    void V2의_프롬프트로_운영체제_중급_난이도_문제를_생성한다() {
        // given
        String prompt = Prompt.V2_PROMPT.generate("운영체제", ProblemDifficulty.EASY);

        // when
        ProblemResponse problemResponse = chatService.sendPrompt(prompt, ProblemResponse.class);

        // then
        assertThat(problemResponse).isNotNull();
        assertThat(problemResponse.title()).isNotEmpty();
        assertThat(problemResponse.description()).isNotEmpty();
        assertThat(problemResponse.aiAnswer()).isNotEmpty();

        System.out.println("=== Gemini API 응답 ===");
        System.out.println(problemResponse);
    }

    @Test
    @Disabled("실제 API 호출 테스트 - 필요할 때만 활성화")
    void V3의_프롬프트로_운영체제_중급_난이도_문제를_생성한다() {
        // given
        String prompt = Prompt.V2_PROMPT.generate("운영체제", ProblemDifficulty.MEDIUM);

        // when
        ProblemResponse problemResponse = chatService.sendPrompt(prompt, ProblemResponse.class);

        // then
        assertThat(problemResponse).isNotNull();
        assertThat(problemResponse.title()).isNotEmpty();
        assertThat(problemResponse.description()).isNotEmpty();
        assertThat(problemResponse.aiAnswer()).isNotEmpty();

        System.out.println("=== Gemini API 응답 ===");
        System.out.println(problemResponse);
    }

    @Test
    @Disabled("실제 API 호출 테스트 - 필요할 때만 활성화")
    void 데이터베이스_고급_난이도_문제를_생성한다() {
        // given
        String prompt = Prompt.V1_PROMPT.generate("MySQL", ProblemDifficulty.HARD);

        // when
        ProblemResponse problemResponse = chatService.sendPrompt(prompt, ProblemResponse.class);

        // then
        assertThat(problemResponse).isNotNull();
        assertThat(problemResponse.title()).isNotEmpty();
        assertThat(problemResponse.description()).isNotEmpty();
        assertThat(problemResponse.aiAnswer()).isNotEmpty();

        System.out.println("=== Gemini API 응답 ===");
        System.out.println(problemResponse);
    }

}