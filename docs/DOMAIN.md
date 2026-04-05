## HaruHaru

## Service 개요

- 금방 포기하는 사람들을 위한 꾸준함을 기르자! 하루 1문제 풀이 서비스
- 부담 없이 가볍게, 경쟁과 비교 없이 오로지 나 자신과의 싸움에 집중
- 매일 1문제 제공, 스트릭(연속 성공 일수) 시스템 도입으로 꾸준한 학습 유도
- 비교와 경쟁 요소 배제, 개인의 성장과 꾸준함에 초점
- AI가 생성한 문제와 예시 답변 제공으로 다양한 주제와 난이도 경험 가능

### 핵심 철학

- 하루 딱 1문제
- 가볍게, 부담 없이
- 경쟁 ❌, 비교 ❌
- 꾸준함(스트릭) ⭕

## Domain 요구사항

## 회원 관리 및 인증
- 회원은 회원가입을 하고 로그인할 수 있다.
    - 회원가입은 로그인 ID, 비밀번호, 닉네임을 입력받아 진행된다.
    - 로그인 ID는 **유일**해야 한다.
    - 비밀번호는 **암호화**되어 저장된다.
    - 닉네임은 **유일해야 한다**.
    - 로그인은 로그인 ID와 비밀번호를 입력받아 진행된다.
    - JWT 토큰 기반 인증이 사용된다.

## 오늘의 문제 설정 및 문제 풀이
#### 오늘의 문제 설정
- 회원은 가입 시 **반드시 오늘의 문제 설정을 완료**해야 한다.
    - 설정 항목은 다음과 같다.
      - 문제 난이도 (EASY / MEDIUM / HARD)
      - 문제 카테고리 (1개 선택)   -> 대분류 -> 중분류 -> 소분류(토픽)
- **Skip**은 불가능하다.
- 설정 직후 오늘의 문제가 제공된다.
  - 처음 설정일 경우에만 즉시 오늘의 문제가 제공된다.
- 설정 변경은 언제든 가능하나 변경 이후 이미 생성된 오늘의 문제에는 영향을 주지 않는다.
- 변경은 **다음날 자정(00:00)** 부터 적용된다.

#### 오늘의 문제 풀이
- 문제는 AI(Google Gemini)가 생성한다. 답변은 예시 형태로 제공된다.
  - 생성 실패 시 백업 문제가 제공된다.
- **매일 자정(00:00)** 에 설정된 난이도와 카테고리에 맞는 문제가 제공된다.
- 동일한 날짜 + 난이도 + 카테고리 조합에 대해 문제는 하나만 생성된다.
<!--- 추후 문제 알림 설정 기능 추가 가능 --->

#### 문제 풀이 제출
- 회원은 제공된 문제를 확인하고 풀이를 제출할 수 있다.
- 답변은 텍스트 형태로 제출된다.
- 답변을 제출할 경우 AI가 생성한 답변이 함께 제공된다.
- 답변 수정은 동일 날짜 내에서만 가능하다.
- 기간이 지난 문제에도 제출은 가능하나 스트릭에는 반영되지 않는다.
- 하루의 기준은 서버 기준 날짜(00:00 ~ 23:59)이다.

#### AI 채점
- 회원이 풀이를 제출(최초 제출 및 수정 제출 포함)하면 AI가 자동으로 채점한다.
- AI는 모범 답안과 회원의 답안을 비교하여 아래 항목을 평가한다.
  - 등급 (EXCELLENT / GOOD / FAIR / POOR)
  - 잘한 점
  - 부족한 점
  - 개선 방향
- 채점은 비동기로 처리되며, 제출 응답과 별개로 완료된다.
- 채점 결과는 제출 이력별로 독립적으로 저장된다. (수정 제출 시 이전 채점 결과 유지)
- 회원은 특정 제출에 대한 채점 이력 전체를 조회할 수 있다.

## 스트릭 시스템 및 알림

#### 스트릭 시스템
- 스트릭이란 회원이 연속으로 문제를 푼 일수를 의미한다.
- 회원이 문제를 푼 날에는 스트릭이 1 증가한다.
- 회원이 문제를 풀지 않은 날에는 스트릭이 0으로 초기화된다

#### 알림
- 알림 종류 및 시점
  - 오늘의 문제 풀이 독려 알림 (UNSOLVED_PROBLEM_REMINDER)
    - 회원이 문제를 풀지 않았을 때 발송된다.
  - 스트릭 유지 실패 임박 알림 (STREAK_REMINDER)
    - 회원이 문제를 풀지 않았을 때 발송된다.
  - 문제 생성 알림 (DAILY_PROBLEM)
    - 매일 자정(00:00)에 새로운 문제가 생성될 때 발송된다.

## 내 기록 조회
- 풀이 기록 목록 조회
  - 조회 가능 항목
    - 날짜, 문제, 제출 여부, 스트릭 반영
- 풀이 상세 조회
  - 특정 날짜의 풀이 상세 내용을 조회
  - 문제 설명, 회원 답변, AI 예시 답변 제공
- 스트릭 현황 조회
  - 숫자 형태로 현재 스트릭 일수를 제공한다.
  - 잔디 형식으로 스트릭 현황을 시각화한다.(추후 구현)

---

## Domain Model

> **공통 필드 (BaseEntity)**
> 모든 엔티티는 아래 필드를 공통으로 가진다.
> - `id` : PK (auto increment)
> - `created_at` : 생성 일시
> - `updated_at` : 수정 일시
> - `status` : 엔티티 상태 (ACTIVE / DELETED) — 논리 삭제

---

### MEMBER (회원)
- `login_id`              : 로그인 ID (최대 50자)
- `password`              : 비밀번호 (암호화 저장)
- `nickname`              : 닉네임 (최대 50자)
- `role`                  : 회원 권한 (ROLE_MEMBER / ROLE_ADMIN)
- `last_login_at`         : 마지막 로그인 일시
- `refresh_token`         : 리프레시 토큰 (해시 저장)
- `profile_image_object_key` : 프로필 이미지 S3 Object Key

#### 규칙
- `login_id`는 유일해야 한다.
- `nickname`은 유일해야 한다.
- `password`는 암호화되어 저장된다.
- `refresh_token`은 해시 처리되어 저장된다.
- 회원 가입 후 권한은 ROLE_MEMBER로 설정된다.

---

### MEMBER_PREFERENCE (회원 설정)
- `member_id`          : 회원 ID (FK → MEMBER)
- `category_topic_id`  : 선호 문제 카테고리 토픽 ID (FK → CATEGORY_TOPIC)
- `difficulty`         : 선호 문제 난이도 (EASY / MEDIUM / HARD)
- `effective_at`       : 설정 적용 시작 날짜

#### 규칙
- 회원은 가입 직후 반드시 오늘의 문제 설정을 완료해야 한다.
- 설정 변경은 언제든 가능하다.
- 변경은 다음날 자정(00:00)부터 적용된다.
- `effective_at`은 해당 설정이 적용되는 시작 날짜를 의미한다.
- 설정 변경 시 `effective_at`은 다음날 날짜로 저장된다.
- 같은 날 설정 변경 시 기존 설정을 업데이트한다(새 레코드 생성 없음).

---

### CATEGORY (카테고리 - 대분류)
- `name` : 카테고리 이름 (예: 개발)

### CATEGORY_GROUP (카테고리 그룹 - 중분류)
- `category_id` : 카테고리 ID (FK → CATEGORY)
- `name`        : 카테고리 그룹 이름 (예: 백엔드, CS, INFRA 등)

### CATEGORY_TOPIC (카테고리 토픽 - 소분류)
- `group_id` : 카테고리 그룹 ID (FK → CATEGORY_GROUP)
- `name`     : 카테고리 토픽 이름 (예: Spring, React, 알고리즘, 데이터베이스, 네트워크, 운영체제 등)

#### 규칙
- 카테고리 계층 구조: CATEGORY(대분류) → CATEGORY_GROUP(중분류) → CATEGORY_TOPIC(소분류)
- 카테고리는 관리자가 관리한다.
- 회원은 CATEGORY_TOPIC 단위로 선호 카테고리를 설정한다.

---

### PROBLEM (문제)
- `title`              : 문제 제목
- `description`        : 문제 설명
- `ai_answer`          : AI 예시 답변 (최대 5000자)
- `category_topic_id`  : 문제 카테고리 토픽 ID (FK → CATEGORY_TOPIC)
- `difficulty`         : 문제 난이도 (EASY / MEDIUM / HARD)
- `problem_at`         : 문제 제공 날짜
- `prompt_version`     : 문제 생성에 사용된 프롬프트 버전

#### 규칙
- 동일한 날짜 + 난이도 + 카테고리 토픽 조합에 대해 문제는 하나만 생성된다.
- 문제는 AI(Google Gemini 2.0 Flash)가 생성한다. 답변은 예시 형태로 제공된다.
- 생성 실패 시 백업 문제가 제공된다.
- 매일 자정(00:00)에 설정된 난이도와 카테고리에 맞는 문제가 제공된다.

---

### DAILY_PROBLEM (오늘의 문제)
- `member_id`  : 회원 ID (FK → MEMBER)
- `problem_id` : 문제 ID (FK → PROBLEM)
- `assigned_at`: 문제 제공 날짜
- `is_solved`  : 문제 풀이 완료 여부

#### 규칙
- 설정 직후 오늘의 문제가 제공된다.
- 회원은 동일 날짜에 하나의 오늘의 문제만 가질 수 있다.
- 제출 완료 시 `is_solved`가 true로 변경된다.
- 인덱스: `idx_member_date (member_id, assigned_at)`

---

### SUBMISSION (문제 풀이 제출)
- `member_id`       : 제출한 회원 ID (FK → MEMBER)
- `daily_problem_id`: 오늘의 문제 ID (FK → DAILY_PROBLEM)
- `answer`          : 제출한 풀이 (최대 5000자)
- `submitted_at`    : 제출 일시
- `is_on_time`      : 기간 내 제출 여부 (스트릭 반영 여부)

#### 규칙
- 답변은 텍스트 형태로 제출된다.
- 답변 수정은 동일 날짜 내에서만 가능하다.
- 자정을 넘긴 이후에는 수정이 불가능하다. 기간이 지난 문제에도 제출은 가능하나 스트릭에는 반영되지 않는다.
- 하루의 기준은 서버 기준 날짜(00:00 ~ 23:59)이다.
- 유니크 제약: `(member_id, daily_problem_id, status)`

---

### SUBMISSION_FEEDBACK (AI 채점 결과)
- `submission_id` : 채점 대상 제출 ID (FK → SUBMISSION)
- `grade`         : 채점 등급 (EXCELLENT / GOOD / FAIR / POOR)
- `strengths`     : 잘한 점
- `weaknesses`    : 부족한 점
- `suggestion`    : 개선 방향
- `graded_at`     : 채점 완료 일시

#### 규칙
- 하나의 제출(SUBMISSION)에 여러 채점 결과가 존재할 수 있다. (최초 제출 + 수정 제출 각각 채점)
- 채점은 제출 완료 이벤트를 통해 비동기로 처리된다.
- 채점 등급:
  - `EXCELLENT` : 모범 답안에 근접한 수준의 완성도 높은 답변
  - `GOOD`      : 핵심 개념을 이해하고 있으나 일부 누락
  - `FAIR`      : 기본 개념은 파악했으나 설명이 부족하거나 불명확
  - `POOR`      : 핵심 개념에 대한 이해가 부족하거나 방향이 틀림

---

### STREAK (스트릭)
- `member_id`       : 회원 ID (FK → MEMBER, unique)
- `current_streak`  : 현재 스트릭 일수 (bigint)
- `max_streak`      : 최대 스트릭 일수 (bigint)
- `last_solved_at`  : 마지막으로 문제를 푼 날짜
- `version`         : 낙관적 락(Optimistic Lock) 버전 (bigint)

#### 규칙
- 스트릭은 문제 제출이 성공적으로 완료된 시점에 갱신된다.
- 회원이 문제를 풀지 않은 날에는 스트릭이 0으로 초기화된다.
- `version` 필드로 동시성 제어(낙관적 락)를 수행한다.
- 회원당 하나의 스트릭 레코드만 존재한다.

---

### NOTIFICATION (알림)
- `member_id` : 알림 수신 회원 ID
- `type`      : 알림 유형 (DAILY_PROBLEM / STREAK_REMINDER / UNSOLVED_PROBLEM_REMINDER)
- `title`     : 알림 제목
- `body`      : 알림 내용
- `is_read`   : 읽음 여부
- `sent_at`   : 알림 발송 일시

#### 규칙
- 동일 유형의 알림은 하루에 한 번만 발송된다.
- 알림 발송 여부는 기록으로 관리한다.
- 알림 유형:
  - `DAILY_PROBLEM` : 매일 자정 새로운 문제가 생성될 때 발송
  - `STREAK_REMINDER` : 스트릭 유지 실패 임박 시 발송
  - `UNSOLVED_PROBLEM_REMINDER` : 오늘의 문제를 풀지 않은 회원에게 독려 알림 발송

---

### MEMBER_DEVICE (회원 디바이스)
- `member_id`     : 회원 ID (FK → MEMBER)
- `device_token`  : 푸시 알림용 디바이스 토큰 (최대 512자)
- `last_synced_at`: 마지막 동기화 일시

#### 규칙
- 회원의 푸시 알림 발송을 위한 디바이스 토큰을 관리한다.
- 회원 한 명이 여러 디바이스를 등록할 수 있다.
- 미사용 디바이스 토큰은 스케줄러를 통해 주기적으로 정리된다.

---

### STORAGE (파일 업로드 이력)
- `member_id`    : 업로드 권한을 발급한 회원 ID
- `target_id`    : 업로드 대상 ID (예: 프로필 이미지의 경우 회원 ID)
- `upload_type`  : 업로드 유형 (PROFILE_IMAGE)
- `upload_status`: 업로드 상태 (ISSUED / COMPLETED / EXPIRED)
- `object_key`   : S3 Object Key (unique, 최대 512자)

#### 규칙
- 서버가 발급한 Presigned URL 업로드 권한에 대한 기록을 관리한다.
- `ISSUED` : Presigned URL 발급 완료 상태
- `COMPLETED` : 실제 파일 업로드 및 완료 처리된 상태
- `EXPIRED` : Presigned URL 유효기간 만료 상태
- `object_key`는 유일해야 한다.
