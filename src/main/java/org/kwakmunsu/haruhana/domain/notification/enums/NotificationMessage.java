package org.kwakmunsu.haruhana.domain.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationMessage {

    DAILY_STUDY_REMINDER      ("오늘의 학습", "오늘의 문제가 기다리고 있어요! 지금 바로 풀어보세요 📚"),
    UNSOLVED_PROBLEM_REMINDER ("미제출 알림", "아직 풀지 않은 문제가 있어요! 하루에 한 문제씩 풀고 스트릭🔥을 유지해보세요!");

    private final String title;
    private final String message;

}