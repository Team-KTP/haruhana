package org.kwakmunsu.haruhana.global.support.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    // COMMON
    BAD_REQUEST            (HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.", LogLevel.INFO),
    UNAUTHORIZED_ERROR     (HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다.", LogLevel.WARN),
    EMPTY_SECURITY_CONTEXT (HttpStatus.UNAUTHORIZED, "Security Context 에 인증 정보가 없습니다.", LogLevel.WARN),
    FORBIDDEN_ERROR        (HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", LogLevel.WARN),
    FORBIDDEN_MODIFY       (HttpStatus.FORBIDDEN, "해당 리소스를 수정할 권한이 없습니다.", LogLevel.WARN),
    FORBIDDEN_DELETE       (HttpStatus.FORBIDDEN, "해당 리소스를 삭제할 권한이 없습니다.", LogLevel.WARN),
    NOT_FOUND              (HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.", LogLevel.INFO),
    DUPLICATE              (HttpStatus.CONFLICT, "이미 존재하는 리소스입니다.", LogLevel.INFO),
    DEFAULT_ERROR          (HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),

    // MEMBER
    INVALID_ACCOUNT                          (HttpStatus.UNAUTHORIZED, "계정 정보가 일치하지 않습니다.", LogLevel.WARN),
    INVALID_NICKNAME                         (HttpStatus.BAD_REQUEST, "사용할 수 없는 닉네임입니다.", LogLevel.INFO),
    EXCEED_MAX_PREFERENCE_COUNT              (HttpStatus.BAD_REQUEST, "회원 학습 정보는 최대 5개까지 등록 가능합니다.", LogLevel.INFO),
    NOT_FOUND_MEMBER                         (HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.", LogLevel.INFO),
    NOT_FOUND_ACTIVE_MEMBER_BY_REFRESH_TOKEN (HttpStatus.NOT_FOUND, "요청하신 Refresh Token 으로 활성화 된 회원을 찾을 수 없습니다.", LogLevel.INFO),
    NOT_FOUND_MEMBER_PREFERENCE              (HttpStatus.NOT_FOUND, "회원 학습 정보를 찾을 수 없습니다.", LogLevel.ERROR), // 회원 정보는 없으면 안되기에 Error
    DUPLICATE_LOGIN_ID                       (HttpStatus.CONFLICT, "이미 사용 중인 로그인 아이디입니다.", LogLevel.INFO),
    DUPLICATE_NICKNAME                       (HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.", LogLevel.INFO),
    DUPLICATE_PREFERENCE                     (HttpStatus.CONFLICT, "이미 등록된 학습정보 입니다.", LogLevel.INFO),

    // MEMBER DEVICE
    NOT_FOUND_MEMBER_DEVICE (HttpStatus.NOT_FOUND, "회원 디바이스 정보를 찾을 수 없습니다.", LogLevel.INFO),

    // STREAK
    NOT_FOUND_STREAK (HttpStatus.NOT_FOUND, "스트릭 정보를 찾을 수 없습니다.", LogLevel.INFO),

    // DAILY PROBLEM
    NOT_FOUND_DAILY_PROBLEM  (HttpStatus.NOT_FOUND, "오늘의 문제를 찾을 수 없습니다.", LogLevel.INFO),

    // PROBLEM
    FAIL_TO_GENERATE_PROBLEM (HttpStatus.INTERNAL_SERVER_ERROR, "문제 생성에 실패했습니다.", LogLevel.ERROR),

    // CATEGORY
    NOT_FOUND_CATEGORY            (HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다.", LogLevel.INFO),
    NOT_FOUND_CATEGORY_GROUP      (HttpStatus.NOT_FOUND, "카테고리 그룹을 찾을 수 없습니다.", LogLevel.INFO),
    NOT_FOUND_CATEGORY_TOPIC      (HttpStatus.NOT_FOUND, "카테고리 주제를 찾을 수 없습니다.", LogLevel.INFO),
    DUPLICATE_CATEGORY_NAME       (HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다.", LogLevel.INFO),
    DUPLICATE_CATEGORY_GROUP_NAME (HttpStatus.CONFLICT, "이미 존재하는 카테고리 그룹 이름입니다.", LogLevel.INFO),
    DUPLICATE_CATEGORY_TOPIC_NAME (HttpStatus.CONFLICT, "이미 존재하는 카테고리 주제 이름입니다.", LogLevel.INFO),

    // NOTIFICATION
    NOT_FOUND_NOTIFICATION (HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.", LogLevel.INFO),
    FCM_SEND_ERROR         (HttpStatus.INTERNAL_SERVER_ERROR, "FCM 메세지 전송에 실패했습니다.", LogLevel.ERROR),

    // STORAGE
    STORAGE_ISSUE_NOT_FOUND(HttpStatus.FORBIDDEN, "해당 이슈의 파일이 존재하지 않거나 접근 권한이 없습니다.", LogLevel.WARN),

    // FIREBASE
    FIREBASE_AUTH_ERROR (HttpStatus.UNAUTHORIZED, "Firebase 인증에 실패했습니다.", LogLevel.WARN),
    INVALID_FCM_TOKEN   (HttpStatus.BAD_REQUEST, "유효하지 않은 FCM 토큰입니다.", LogLevel.INFO),
    NOT_FOUND_FCM_TOKEN (HttpStatus.NOT_FOUND, "FCM 토큰을 찾을 수 없습니다.", LogLevel.INFO),
    FIREBASE_INIT_ERROR (HttpStatus.INTERNAL_SERVER_ERROR, "Firebase 초기화에 실패했습니다.", LogLevel.ERROR),

    // JWT
    EMPTY_TOKEN                        (HttpStatus.UNAUTHORIZED, "JWT 토큰이 존재하지 않습니다.", LogLevel.WARN),
    INVALID_TOKEN                      (HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT 토큰입니다.", LogLevel.WARN),
    TOKEN_THEFT_DETECTED               (HttpStatus.UNAUTHORIZED, "토큰 탈취가 감지되었습니다. 보안을 위해 재로그인이 필요합니다.", LogLevel.WARN),
    NOT_FOUND_TOKEN                    (HttpStatus.NOT_FOUND, "토큰을 찾을 수 없습니다.", LogLevel.INFO),
    CONCURRENT_REQUESTS_LIMIT_EXCEEDED (HttpStatus.TOO_MANY_REQUESTS, "동시에 여러 토큰 재발급 요청이 감지되었습니다. 잠시 후 다시 시도해주세요.", LogLevel.WARN),

    // AWS
    INVALID_FILE_EXTENSION (HttpStatus.BAD_REQUEST, "지원하지 않는 파일 확장자입니다.", LogLevel.INFO),
    INVALID_S3_URL         (HttpStatus.BAD_REQUEST, "AWS S3 URL이 올바르지 않습니다.", LogLevel.INFO),
    NOT_FOUND_FILE         (HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다.", LogLevel.INFO),
    AWS_S3_ERROR           (HttpStatus.INTERNAL_SERVER_ERROR, "AWS S3 내부 에러", LogLevel.ERROR),
    FAILED_TO_UPLOAD_FILE  (HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패하였습니다.", LogLevel.ERROR),
    FILE_SIZE_EXCEEDED     (HttpStatus.INTERNAL_SERVER_ERROR, "파일 크기가 허용된 최대 크기를 초과하였습니다.", LogLevel.ERROR),
    S3_PRESIGNED_URL_ERROR (HttpStatus.INTERNAL_SERVER_ERROR,"presigned-url 생성에 실패하였습니다." , LogLevel.ERROR),

    ;
    private final HttpStatus status;
    private final String message;
    private final LogLevel logLevel;

}