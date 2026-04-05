package org.kwakmunsu.haruhana.domain.submission.event;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.UnitTestSupport;
import org.kwakmunsu.haruhana.domain.submission.service.FeedbackGradingService;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * FeedbackEventHandler 유닛 테스트
 * - 이벤트 수신 시 채점 서비스 위임 검증
 */
class FeedbackEventHandlerUnitTest extends UnitTestSupport {

    @Mock
    FeedbackGradingService feedbackGradingService;

    @InjectMocks
    FeedbackEventHandler feedbackEventHandler;

    @Test
    void 이벤트_수신_시_채점_서비스를_호출한다() {
        // given
        var event = SubmissionCompletedEvent.of(1L, 1L, true, true);

        // when
        feedbackEventHandler.handleSubmissionCompleted(event);

        // then
        verify(feedbackGradingService, times(1)).grade(1L);
    }

}
