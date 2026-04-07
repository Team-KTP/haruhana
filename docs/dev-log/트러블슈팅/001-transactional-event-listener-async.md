# 001. `@TransactionalEventListener(AFTER_COMMIT)` + 트랜잭션 전파

> **카테고리**: 트러블슈팅  
> **날짜**: 2026-04-04  
> **관련 PR**: #201 (AI 채점 기능)

---

## 배경

AI 채점 기능 구현 중 이벤트 기반 비동기 처리를 설계하면서 두 가지 질문이 생겼다.

1. `@EventListener` vs `@TransactionalEventListener(AFTER_COMMIT)` 중 무엇을 써야 하나?
2. `AFTER_COMMIT` 이후에 `@Transactional`이 붙은 메서드를 호출하면 트랜잭션은 어떻게 열리나?

---

## 문제 / 목표

- **이벤트 발행 시점 이해**: `publishEvent()`는 즉시 리턴하고, 원본 트랜잭션은 핸들러와 무관하게 커밋된다.
- **리스너 실행 시점 차이** 파악 및 올바른 선택.
- `AFTER_COMMIT` 이후 비동기 핸들러에서 트랜잭션이 어떻게 동작하는지 검증.

---

## `@EventListener` vs `@TransactionalEventListener` 차이

| | `@EventListener` | `@TransactionalEventListener(AFTER_COMMIT)` |
|--|--|--|
| 리스너 실행 시점 | `publishEvent()` 호출 즉시 | 원본 트랜잭션 커밋 완료 후 |
| `@Async` 없을 때 위험 | 커밋 전 데이터 조회 가능 | 안전 |
| `@Async` 있을 때 위험 | 커밋 전에 새 스레드 시작 가능 | 안전 |

`@Async`가 있어도 스레드 시작 자체는 `publishEvent()` 시점에 이루어지므로 원본 트랜잭션 커밋 전에 핸들러가 DB를 조회할 수 있다. → **데이터 일관성 보장이 필요한 경우 `@TransactionalEventListener(AFTER_COMMIT)` 사용**

---

## Before (문제 케이스 1) — 동기 + AFTER_COMMIT

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
// @Async 없음 → 원본 스레드에서 그대로 실행
public void onEvent(MyEvent event) {
    myService.doSomething(); // @Transactional REQUIRED
}
```

**흐름:**

```
TX1 커밋 완료
  → 같은 스레드에서 핸들러 실행
  → ThreadLocal에 TX1 잔재가 남아있음
  → Spring: "트랜잭션 있네?" → TX1에 join
  → TX1은 이미 커밋됨 → save() 호출해도 실제 INSERT 없음
```

**증상**: 로그는 찍히는데 DB에 데이터가 없음.

---

## After (해결 1) — 동기 방식이라면 `REQUIRES_NEW` 필수

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)  // 강제로 새 트랜잭션 생성
public void onEvent(MyEvent event) {
    myService.doSomething();
}
```

---

## Before (문제 케이스 2) — 비동기 + AFTER_COMMIT (실제 채택한 방식 검토)

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleSubmissionCompleted(SubmissionCompletedEvent event) {
    feedbackGradingService.grade(event.submissionId()); // @Transactional REQUIRED
}
```

의문: `@Async` + `AFTER_COMMIT`이면 `REQUIRES_NEW`가 필요한가?

---

## After (해결 2) — 비동기 방식은 `REQUIRED` 기본값으로 충분

**흐름:**

```
TX1 커밋 완료
  → 새 스레드에서 핸들러 실행
  → ThreadLocal 완전히 비어있음 (새 스레드)
  → grade() 진입 → @Transactional AOP 프록시 동작
  → Spring: "트랜잭션 없네" → REQUIRED가 새 트랜잭션(TX2) 생성
  → grade() 내에서 정상 커밋
```

트랜잭션을 여는 주체는 `@Async`가 아니라 **`grade()`의 `@Transactional`**이다.  
`@Async`는 새 스레드를 사용해 ThreadLocal을 깨끗하게 만들어줄 뿐이다.

---

## 왜 이렇게 했는가

| 조합 | ThreadLocal 상태 | REQUIRED 동작 | 필요한 propagation |
|------|----------------|--------------|-----------------|
| 동기 + AFTER_COMMIT | TX1 잔재 있음 | TX1에 join → 문제 | `REQUIRES_NEW` |
| `@Async` + AFTER_COMMIT | 깨끗 (새 스레드) | 새 트랜잭션 생성 → 정상 | `REQUIRED` (기본값) |

> **핵심 원칙**: 스프링 트랜잭션은 ThreadLocal 기반이다.
> `@Async`는 새 스레드를 사용하므로 자동으로 트랜잭션 컨텍스트가 격리된다.

---

## 현재 구현 (`FeedbackEventHandler`)

```java
@Slf4j
@RequiredArgsConstructor
@Component
public class FeedbackEventHandler {

    private final FeedbackGradingService feedbackGradingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubmissionCompleted(SubmissionCompletedEvent event) {
        log.info("[FeedbackEventHandler] AI 채점 시작 - submissionId: {}", event.submissionId());
        feedbackGradingService.grade(event.submissionId());
    }
}
```

- `@Async` → 새 스레드 → ThreadLocal 비어있음
- `grade()`의 `@Transactional` → Spring AOP 프록시가 새 트랜잭션 생성 → `REQUIRES_NEW` 불필요
- `@TransactionalEventListener(AFTER_COMMIT)` → submission 커밋 후 실행 보장 → 데이터 조회 안전

---

## 배운 점 / 회고

- 동기/비동기 방식에 따라 `AFTER_COMMIT` 이후 트랜잭션 동작이 완전히 달라진다.
- 스프링 트랜잭션이 ThreadLocal 기반이라는 사실을 이해하면 전파 동작이 직관적으로 이해된다.
- `@Async`의 역할은 "새 트랜잭션 생성"이 아니라 "ThreadLocal 격리"임을 명확히 구분할 필요가 있다.
