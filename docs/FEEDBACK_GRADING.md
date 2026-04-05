# AI 채점 기능 구현 계획

## 개요

회원이 풀이를 제출하면 AI가 자동으로 채점하고 상세 피드백을 제공하는 기능.

---

## 설계 결정 및 근거

### 1. `SubmissionFeedback` 별도 엔티티 분리

`Submission`에 채점 필드를 추가하지 않고 별도 엔티티로 분리한다.

**근거**
- `Submission`의 책임은 "언제, 어떤 답을 제출했는가"이고, `SubmissionFeedback`의 책임은 "AI가 그 답을 어떻게 평가했는가"로 관심사가 다르다.
- 제출 시점과 채점 완료 시점이 다르다. (비동기 처리)
- 채점이 없는 `Submission`도 존재할 수 있어, 필드로 넣으면 nullable 필드가 증가한다.
- 채점 로직(프롬프트 버전, 모델 교체 등)을 독립적으로 변경할 수 있다.

### 2. Submission : SubmissionFeedback = 1:N 관계

**근거**
- 답안 수정 제출마다 새로운 채점 결과를 생성한다.
- 채점 이력이 보존되어 "내 답변이 수정하면서 어떻게 개선됐는지" 확인할 수 있다.
- 학습 서비스의 핵심 가치(성장 추적)와 부합한다.

### 3. 제출 시 자동 채점 (이벤트 기반 비동기)

별도 채점 요청 API를 두지 않고, 제출 시 자동으로 채점한다.

**근거**
- 사용자 경험 측면에서 "제출 → 자동 채점"이 가장 자연스러운 흐름이다.
- 기존 `SubmissionCompletedEvent` + `@Async @EventListener` 패턴을 그대로 활용한다.
- 비동기 처리로 제출 응답 속도에 영향을 주지 않는다.
- 최초 제출과 수정 제출 모두 채점한다.

### 4. 기존 `SubmissionCompletedEvent` 재사용

새 이벤트를 만들지 않고 기존 이벤트에 `FeedbackEventHandler`를 추가한다.

**근거**
- 이미 `submissionId`를 포함하고 있어 채점에 필요한 데이터를 조회할 수 있다.
- Streak 핸들러와 채점 핸들러가 동일 이벤트를 독립적으로 수신한다.

---

## 전체 흐름

```
POST /v1/daily-problem/{dailyProblemId}/submissions
  ↓
SubmissionService.submitSolution()
  ├── SubmissionManager.submit()         → Submission 저장/업데이트
  └── ApplicationEventPublisher          → SubmissionCompletedEvent 발행
        ↓ (async)
        ├── SubmissionEventHandler       → Streak 처리 (기존)
        └── FeedbackEventHandler         → 채점 트리거 (신규)
              ↓
              FeedbackGradingService.grade(submissionId)
                ├── SubmissionReader.findWithProblem(submissionId)
                ├── Prompt.GRADING_PROMPT 생성
                ├── ChatService.sendPrompt()  → AI 채점
                └── SubmissionFeedbackRepository.save()

GET /v1/submissions/{submissionId}/feedbacks
  ↓
  채점 이력 목록 반환
```

---

## 생성 파일 목록

| 파일 | 패키지 | 역할 |
|------|--------|------|
| `FeedbackGrade.java` | `domain/submission/enums` | 채점 등급 enum |
| `SubmissionFeedback.java` | `domain/submission/entity` | 채점 결과 엔티티 |
| `FeedbackGradingResult.java` | `domain/submission/service/dto` | AI 응답 매핑 DTO |
| `FeedbackResponse.java` | `domain/submission/service/dto/response` | API 응답 DTO |
| `SubmissionFeedbackJpaRepository.java` | `domain/submission/repository` | 채점 결과 레포지토리 |
| `FeedbackGradingService.java` | `domain/submission/service` | AI 호출 + 저장 |
| `FeedbackEventHandler.java` | `domain/submission/event` | 이벤트 수신 → 채점 트리거 |
| `SubmissionFeedbackController.java` | `domain/submission/controller` | 채점 이력 조회 API |
| `SubmissionFeedbackDocsController.java` | `domain/submission/controller` | Swagger 문서 |

## 수정 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `Prompt.java` | `GRADING_PROMPT` 추가 |
| `SubmissionReader.java` | `findWithProblem(submissionId)` 추가 (Problem fetch join) |

---

## 도메인 모델

```
SUBMISSION (1) ──── (N) SUBMISSION_FEEDBACK
     │                        │
  답안 제출 책임            채점 결과 책임
  (answer, submittedAt)   (grade, strengths, weaknesses, suggestion, gradedAt)
```

---

## AI 채점 프롬프트 구조

`Prompt.GRADING_PROMPT` 입력:
- `{problemDescription}` : 문제 내용
- `{aiAnswer}` : AI 모범 답안
- `{userAnswer}` : 회원 제출 답안

AI 출력 (`FeedbackGradingResult`):
```json
{
  "grade": "GOOD",
  "strengths": "핵심 개념을 명확히 정의하고...",
  "weaknesses": "동작 원리에 대한 설명이 부족하며...",
  "suggestion": "트랜잭션 전파 속성과 격리 수준을 함께 언급하면..."
}
```

---

## API 명세

### 채점 이력 조회

```
GET /v1/submissions/{submissionId}/feedbacks
Authorization: Bearer {token}
```

**Response**
```json
[
  {
    "feedbackId": 2,
    "grade": "GOOD",
    "strengths": "...",
    "weaknesses": "...",
    "suggestion": "...",
    "gradedAt": "2026-04-04T10:32:00"
  },
  {
    "feedbackId": 1,
    "grade": "FAIR",
    "strengths": "...",
    "weaknesses": "...",
    "suggestion": "...",
    "gradedAt": "2026-04-04T09:15:00"
  }
]
```

- 최신 채점 결과가 먼저 반환된다. (`gradedAt` DESC)
- 채점 진행 중인 경우 빈 배열 반환.
