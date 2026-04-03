package org.kwakmunsu.haruhana.domain.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.UnitTestSupport;
import org.kwakmunsu.haruhana.domain.dailyproblem.service.DailyProblemReader;
import org.kwakmunsu.haruhana.domain.member.service.MemberDeviceReader;
import org.kwakmunsu.haruhana.domain.notification.enums.NotificationMessage;
import org.kwakmunsu.haruhana.domain.notification.enums.NotificationType;
import org.kwakmunsu.haruhana.global.support.notification.ErrorNotificationSender;
import org.kwakmunsu.haruhana.global.support.notification.NotificationSender;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class NotificationSchedulerUnitTest extends UnitTestSupport {

    @Mock
    DailyProblemReader dailyProblemReader;

    @Mock
    MemberDeviceReader memberDeviceReader;

    @Mock
    NotificationSender notificationSender;

    @Mock
    ErrorNotificationSender errorNotificationSender;

    @InjectMocks
    NotificationScheduler notificationScheduler;

    @Test
    void 오전_학습_독려_알림_토큰_수만큼_전송된다() {
        // given
        List<String> deviceTokens = List.of("token-1", "token-2", "token-3");

        when(memberDeviceReader.findAllActiveDeviceTokens()).thenReturn(deviceTokens);
        doNothing().when(notificationSender).sendNotification(any(), any(), any(), any());

        // when
        notificationScheduler.notifyDailyStudyReminder();

        // then
        verify(notificationSender, times(3)).sendNotification(
                any(),
                eq(NotificationMessage.DAILY_STUDY_REMINDER.getTitle()),
                eq(NotificationMessage.DAILY_STUDY_REMINDER.getMessage()),
                eq(NotificationType.DAILY_STUDY_REMINDER)
        );
    }

    @Test
    void 활성_디바이스_토큰이_없을_때_오전_알림이_전송되지_않는다() {
        // given
        when(memberDeviceReader.findAllActiveDeviceTokens()).thenReturn(List.of());

        // when
        notificationScheduler.notifyDailyStudyReminder();

        // then
        verify(notificationSender, never()).sendNotification(any(), any(), any(), any());
    }

    @Test
    void 오전_알림_전송_중_예외_발생_시에도_스케줄러가_정상_종료된다() {
        // given
        List<String> deviceTokens = List.of("token-1");

        when(memberDeviceReader.findAllActiveDeviceTokens()).thenReturn(deviceTokens);
        doThrow(new RuntimeException("FCM 전송 실패"))
                .when(notificationSender).sendNotification(any(), any(), any(), any());

        // when & then: 예외가 외부로 전파되지 않음
        notificationScheduler.notifyDailyStudyReminder();

        verify(errorNotificationSender, times(1)).sendErrorNotification(any(), any());
    }

    @Test
    void 오전_알림_토큰_조회_중_예외_발생_시에도_스케줄러가_정상_종료된다() {
        // given
        doThrow(new RuntimeException("DB 조회 실패"))
                .when(memberDeviceReader).findAllActiveDeviceTokens();

        // when & then: 예외가 외부로 전파되지 않음
        notificationScheduler.notifyDailyStudyReminder();

        verify(notificationSender, never()).sendNotification(any(), any(), any(), any());
        verify(errorNotificationSender, times(1)).sendErrorNotification(any(), any());
    }

    @Test
    void 미풀이_회원이_있을_때_디바이스_토큰_수만큼_알림이_전송된다() {
        // given
        List<Long> unsolvedMemberIds = List.of(1L, 2L);
        List<String> deviceTokens = List.of("token-1", "token-2", "token-3");

        when(dailyProblemReader.findUnsolvedMember(any(LocalDate.class))).thenReturn(unsolvedMemberIds);
        when(memberDeviceReader.findDeviceTokensByMemberIds(unsolvedMemberIds)).thenReturn(deviceTokens);
        doNothing().when(notificationSender).sendNotification(any(), any(), any(), any());

        // when
        notificationScheduler.notifyUnsolvedProblemMembers();

        // then
        verify(notificationSender, times(3)).sendNotification(
                any(),
                eq(NotificationMessage.UNSOLVED_PROBLEM_REMINDER.getTitle()),
                eq(NotificationMessage.UNSOLVED_PROBLEM_REMINDER.getMessage()),
                eq(NotificationType.UNSOLVED_PROBLEM_REMINDER)
        );
    }

    @Test
    void 미풀이_회원이_없을_때_알림이_전송되지_않는다() {
        // given
        when(dailyProblemReader.findUnsolvedMember(any(LocalDate.class))).thenReturn(List.of());

        // when
        notificationScheduler.notifyUnsolvedProblemMembers();

        // then
        verify(memberDeviceReader, never()).findDeviceTokensByMemberIds(any());
        verify(notificationSender, never()).sendNotification(any(), any(), any(), any());
    }

    @Test
    void 알림_전송_중_예외_발생_시에도_스케줄러가_정상_종료된다() {
        // given
        List<Long> unsolvedMemberIds = List.of(1L);
        List<String> deviceTokens = List.of("token-1");

        when(dailyProblemReader.findUnsolvedMember(any(LocalDate.class))).thenReturn(unsolvedMemberIds);
        when(memberDeviceReader.findDeviceTokensByMemberIds(unsolvedMemberIds)).thenReturn(deviceTokens);
        doThrow(new RuntimeException("FCM 전송 실패"))
                .when(notificationSender).sendNotification(any(), any(), any(), any());

        // when & then: 예외가 외부로 전파되지 않음
        notificationScheduler.notifyUnsolvedProblemMembers();

        verify(errorNotificationSender, times(1)).sendErrorNotification(any(), any());
    }

    @Test
    void 미풀이_회원_조회_중_예외_발생_시에도_스케줄러가_정상_종료된다() {
        // given
        doThrow(new RuntimeException("DB 조회 실패"))
                .when(dailyProblemReader).findUnsolvedMember(any(LocalDate.class));

        // when & then: 예외가 외부로 전파되지 않음
        notificationScheduler.notifyUnsolvedProblemMembers();

        verify(notificationSender, never()).sendNotification(any(), any(), any(), any());
        verify(errorNotificationSender, times(1)).sendErrorNotification(any(), any());
    }

}