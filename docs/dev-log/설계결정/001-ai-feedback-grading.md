# 001. AI 채점 기능 설계 — 엔티티 분리, 1:N 관계, 이벤트 재활용

> **카테고리**: 설계결정  
> **날짜**: 2026-04-04  
> **관련 PR**: #201

---

## 배경

회원이 풀이를 제출하면 AI가 자동으로 채점하고 상세 피드백을 제공하는 기능을 설계했다. 구현 방향을 결정하기 전에 몇 가지 설계 선택지를 검토했다.

---

## 핵심 설계 결정

### 1. `SubmissionFeedback` 별도 엔티티 분리

`Submission`에 채점 필드를 추가하지 않고 별도 엔티티로 분리한다.

**근거**
- `Submission`의 책임은 "언제, 어떤 답을 제출했는가", `SubmissionFeedback`의 책임은 "AI가 그 답을 어떻게 평가했는가"로 관심사가 다르다.
- 제출 시점과 채점 완료 시점이 다르다. (비동기 처리)
- 채점이 없는 `Submission`도 존재할 수 있어, 필드로 넣으면 nullable 필드가 증가한다.
- 채점 로직(프롬프트 버전, 모델 교체 등)을 독립적으로 변경할 수 있다.

### 2. `Submission : SubmissionFeedback = 1:N` 관계

**근거**
- 답안 수정 제출마다 새로운 채점 결과를 생성한다.
- 채점 이력이 보존되어 "내 답변이 수정하면서 어떻게 개선됐는지" 확인할 수 있다.
- 학습 서비스의 핵심 가치(성장 추적)와 부합한다.

### 3. 제출 시 자동 채점 (이벤트 기반 비동기)

별도 채점 요청 API를 두지 않고, 제출 시 자동으로 채점한다.

**근거**
- 사용자 경험 측면에서 "제출 → 자동 채점"이 가장 자연스러운 흐름이다.
- 기존 `SubmissionCompletedEvent` + `@Async @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 패턴을 활용한다.
- 비동기 처리로 제출 응답 속도에 영향을 주지 않는다.
- 최초 제출과 수정 제출 모두 채점한다.

### 4. 기존 `SubmissionCompletedEvent` 재사용

새 이벤트를 만들지 않고 기존 이벤트에 `FeedbackEventHandler`를 추가한다.

**근거**
- 이미 `submissionId`를 포함하고 있어 채점에 필요한 데이터를 조회할 수 있다.
- Streak 핸들러와 채점 핸들러가 동일 이벤트를 독립적으로 수신한다.
- 새로운 후속 처리 추가 시 기존 코드를 수정하지 않아도 된다. (OCP)

---

## Before (기존 흐름)

```
POST /v1/daily-problem/{dailyProblemId}/submissions
  ↓
SubmissionService.submitSolution()
  ├── SubmissionManager.submit()       → Submission 저장/업데이트
  └── ApplicationEventPublisher        → SubmissionCompletedEvent 발행
        ↓ (async)
        └── SubmissionEventHandler     → Streak 처리만 존재
```

---

## After (채점 핸들러 추가)

```
POST /v1/daily-problem/{dailyProblemId}/submissions
  ↓
SubmissionService.submitSolution()
  ├── SubmissionManager.submit()           → Submission 저장/업데이트
  └── ApplicationEventPublisher            → SubmissionCompletedEvent 발행
        ↓ (async)
        ├── SubmissionEventHandler         → Streak 처리 (기존 — 변경 없음)
        └── FeedbackEventHandler           → 채점 트리거 (신규)
              ↓
              FeedbackGradingService.grade(submissionId)
                ├── SubmissionReader.findWithProblem(submissionId)
                ├── Prompt.GRADING_PROMPT 생성
                ├── ChatService.sendPrompt()  → AI 채점
                └── SubmissionFeedbackRepository.save()

GET /v1/submissions/{submissionId}/feedbacks
  ↓
  채점 이력 목록 반환 (gradedAt DESC)
```

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

**GRADING_PROMPT 입력:**
- `{problemDescription}` : 문제 내용
- `{aiAnswer}` : AI 모범 답안
- `{userAnswer}` : 회원 제출 답안

**AI 출력 (`FeedbackGradingResult`):**
```json
{
  "grade": "GOOD",
  "strengths": "핵심 개념을 명확히 정의하고...",
  "weaknesses": "동작 원리에 대한 설명이 부족하며...",
  "suggestion": "트랜잭션 전파 속성과 격리 수준을 함께 언급하면..."
}
```

**프롬프트 설계 원칙:**
- 프롬프트 인젝션 방어 로직 포함
- 모범 답안을 참고만 하고 독립적 평가 강조
- 객관성과 정확성 우선
- 회원 답안이 모범 답안보다 우수할 수 있음을 허용

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

---

## 배운 점 / 회고

- 엔티티 분리를 "지금 당장 필요한가"로 판단하면 Submission 필드 추가가 더 간단해 보인다. 하지만 "책임이 다른가"와 "독립적으로 변경될 가능성이 있는가"를 기준으로 판단하면 분리가 맞다.
- 이벤트 재활용은 새 이벤트 타입을 만드는 것보다 코드 변경을 최소화하지만, 이벤트가 의미상 단일 책임을 유지하는지 주기적으로 검토해야 한다.
- 비동기 채점은 제출 응답 속도에 영향을 주지 않지만, 사용자가 "제출 직후 피드백이 없다"는 상황에 익숙해야 한다. UX 관점에서 스켈레톤 UI나 로딩 상태 표시가 필요할 수 있다.
