# 002. 회원 학습 설정 추가 — TOCTOU Race Condition

> **카테고리**: 트러블슈팅  
> **날짜**: 2026-04-05  
> **관련 PR**: #197

---

## 배경

회원 학습 선호 정보 추가 등록 기능(`POST /v1/members/preferences`) 구현 중 발견된 문제들과 해결 과정을 기록한다.

---

## 이슈 1. 메서드명과 로직 불일치 — `isEffectiveToday()`

### 문제

`MemberPreference.updatePreference()` 흐름에서 기존 설정을 인플레이스로 수정할지, 소프트 삭제 후 내일 날짜로 재생성할지를 분기하는 메서드가 있었다.

```java
// MemberPreference.java
public boolean isEffectiveToday() {
    return this.effectiveAt.isEqual(LocalDate.now().plusDays(1)); // 내일 날짜와 비교
}
```

메서드명만 보면 "오늘 적용 중인지"를 검사하는 것처럼 읽힌다. 실제로는 **내일 날짜와 비교**하고 있어 읽는 사람이 혼란을 느낀다.

### 원인

배치 잡이 자정(00:00)에 선호 설정을 다음 날짜(`effectiveAt = tomorrow`)로 미리 생성한다. 이미 내일 날짜로 예약된 레코드가 있으면 인플레이스 수정, 오늘 날짜 레코드만 있으면 소프트 삭제 후 재생성하는 것이 올바른 동작이다. 로직 자체는 맞지만 **메서드명이 의도를 잘못 표현**하고 있었다.

### Before

```java
public boolean isEffectiveToday() {
    return this.effectiveAt.isEqual(LocalDate.now().plusDays(1));
}
```

### After

```java
public boolean isScheduledForTomorrow() {
    return this.effectiveAt.isEqual(LocalDate.now().plusDays(1));
}
```

```java
// MemberManager.java 주석도 함께 수정
// Before: "같은 날짜에 회원 설정 변경 시 기존 설정을 업데이트하는 방식으로 처리"
// After:  "이미 내일 날짜로 예약된 설정이 있으면 인플레이스 수정, 아니면 소프트 삭제 후 내일 날짜로 재생성"
if (memberPreference.isScheduledForTomorrow()) { ... }
```

---

## 이슈 2. TOCTOU(Time-Of-Check-Time-Of-Use) Race Condition

### 문제

`validateAppendPreference()`에서 count 체크와 실제 save 사이에 다른 요청이 끼어들 수 있다.

```java
// MemberValidator.java
int currentPreferenceCount = memberPreferenceJpaRepository.countByMemberIdAndStatus(memberId, ACTIVE);
if (currentPreferenceCount >= MAX_PREFERENCE_COUNT) { // MAX = 5
    throw new HaruHanaException(ErrorType.EXCEED_MAX_PREFERENCE_COUNT);
}
// ↑ 체크 통과 후 ↓ save 전 사이에 다른 요청이 끼어들 수 있음
memberManager.registerPreference(member, newPreference); // save
```

**동시 요청 시나리오:**

| 시간 | 요청 A | 요청 B |
|------|--------|--------|
| t1 | count 조회 → 4 (통과) | |
| t2 | | count 조회 → 4 (통과) |
| t3 | save → count 5 | |
| t4 | | save → count **6 (MAX 초과!)** |

### 원인

MySQL 기본 격리 수준(Repeatable Read)에서 count 체크와 save가 별개의 visibility로 동작한다. 트랜잭션 내에서 count를 읽더라도 아직 커밋되지 않은 다른 트랜잭션의 INSERT는 보이지 않는다.

### 해결 방안 비교

| 방안 | 설명 | 채택 여부 |
|------|------|---------|
| DB 유니크 제약 | 중복 카테고리 방지에는 유효하지만 count 제한에는 무용 | 부분 적용 |
| Redis 분산 락 | 확실하지만 인프라 의존성 추가, 장애 전파 위험 | 미채택 |
| `synchronized` | 단일 인스턴스에서만 유효, 멀티 인스턴스 환경 불가 | 미채택 |
| **비관적 락** | DB 레벨에서 Member row를 잠가 동시 요청 직렬화 | **채택** |

### Before

```java
@Transactional
public void appendPreference(NewPreference newPreference, Long memberId) {
    Member member = memberReader.find(memberId);        // 일반 조회
    memberValidator.validateAppendPreference(newPreference, memberId);
    memberManager.registerPreference(member, newPreference);
    ...
}
```

### After

```java
// MemberJpaRepository.java — 비관적 락 쿼리 추가
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT m FROM Member m WHERE m.id = :id AND m.status = :status")
Optional<Member> findByIdAndStatusWithLock(@Param("id") Long id, @Param("status") EntityStatus status);

// MemberService.java — 락 먼저 획득 후 검증
@Transactional
public void appendPreference(NewPreference newPreference, Long memberId) {
    Member member = memberReader.findWithLock(memberId); // SELECT ... FOR UPDATE
    memberValidator.validateAppendPreference(newPreference, memberId);
    memberManager.registerPreference(member, newPreference);
    ...
}
```

**직렬화 후 동시 요청 흐름:**

| 시간 | 요청 A | 요청 B |
|------|--------|--------|
| t1 | findWithLock() → 락 획득 | |
| t2 | | findWithLock() → **대기 (블로킹)** |
| t3 | count=4 → 통과 → save → 커밋 (락 해제) | |
| t4 | | 락 획득 → count=5 → `EXCEED_MAX_PREFERENCE_COUNT` 예외 |

**이 방식의 장점:**
- 락이 Member row 단위 → 다른 회원 간에는 블로킹 없음
- 트랜잭션 범위가 짧아 락 점유 시간 최소화
- Redis 등 외부 인프라 없이 DB 레벨에서 해결

---

## 이슈 3. `MemberManager.updatePreference()`의 중복 `@Transactional`

### 문제

```java
// MemberService.java
@Transactional
public void updatePreference(UpdatePreference updatePreference, Long memberId) {
    ...
    memberManager.updatePreference(memberPreference, updatePreference);
}

// MemberManager.java
@Transactional  // ← 중복, 실질적으로 아무 효과 없음
public void updatePreference(MemberPreference memberPreference, UpdatePreference updatePreference) { ... }
```

Spring Bean 간 호출이므로 프록시를 통해 트랜잭션이 전파된다. `MemberManager.updatePreference()`의 `@Transactional`은 기본 전파 수준 `REQUIRED`에 의해 항상 외부 트랜잭션에 합류하므로 실질적으로 아무 효과가 없다.

### 해결

트랜잭션 경계는 Service 레이어에서만 관리하는 원칙으로 통일.

> **원칙**: 트랜잭션 경계는 Service 레이어에서 선언. Manager / Reader / Validator는 트랜잭션을 소유하지 않는다.

---

## 관련 파일

| 파일 | 변경 내용 |
|------|---------|
| `MemberPreference.java` | `isEffectiveToday()` → `isScheduledForTomorrow()` 리네이밍 |
| `MemberManager.java` | 호출부 메서드명 및 주석 수정 |
| `MemberJpaRepository.java` | `findByIdAndStatusWithLock()` 추가 |
| `MemberReader.java` | `findWithLock()` 추가 |
| `MemberService.java` | `appendPreference()` — 락 먼저 획득, 로그 추가 |

---

## 배운 점 / 회고

- count 체크 + save가 같은 트랜잭션 안에 있어도 TOCTOU는 발생할 수 있다. 격리 수준과 visibility를 이해해야 한다.
- 비관적 락은 "무거운 해결책"처럼 느껴지지만, 락 범위(Member row 단위)와 트랜잭션 수명을 적절히 설계하면 오버헤드를 최소화할 수 있다.
- 메서드명은 코드의 의도를 정확히 표현해야 한다. 이름 하나가 읽는 사람의 이해를 완전히 바꾼다.
