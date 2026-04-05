package org.kwakmunsu.haruhana.infrastructure.firebase;

import org.junit.jupiter.api.Test;
import org.kwakmunsu.haruhana.domain.notification.enums.NotificationType;
import org.kwakmunsu.haruhana.global.support.notification.NotificationSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 실제 FCM 푸시 알림 전송 테스트
 * - 평소에는 @Disabled 로 CI/CD에서 실행 제외
 * - 수동으로 확인하고 싶을 때 @Disabled 제거 후 실행
 * - FCM_TOKEN 에 클라이언트 기기 토큰을 입력 후 실행
 */
//@Disabled("수동 실행 전용: FCM 실제 전송 테스트")
@ActiveProfiles("local")
@SpringBootTest
class FcmNotificationRealSendTest {

    private static final String FCM_TOKEN = "coRhU3egkYP0joHTwqhSFi:APA91bGA3odis_X8SaFa92Gq-fE85mrBVpykbU5542v8ZX3N5kWC-tWJUAI-kgRAS0edKMsj52a-6Kig_cX428JSu44LGjeOcH-7sjnWZ750ZI103sIlsA4";

    @Autowired
    private NotificationSender notificationSender;

    @Test
    void 오늘의_문제_알림_전송() {
        notificationSender.sendNotification(
                FCM_TOKEN,
                "오늘의 문제가 도착했어요!",
                "지금 바로 풀어보세요.",
                NotificationType.DAILY_PROBLEM
        );
    }

    @Test
    void 스트릭_유지_독려_알림_전송() {
        notificationSender.sendNotification(
                FCM_TOKEN,
                "스트릭이 끊길 위기예요!",
                "오늘 문제를 풀고 스트릭을 지켜보세요.",
                NotificationType.STREAK_REMINDER
        );
    }

    @Test
    void 미해결_문제_독려_알림_전송() {
        notificationSender.sendNotification(
                FCM_TOKEN,
                "아직 못 푼 문제가 있어요.",
                "오늘 안에 도전해보세요!",
                NotificationType.UNSOLVED_PROBLEM_REMINDER
        );
    }

}
