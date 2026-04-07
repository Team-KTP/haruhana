# 004. 비동기 문제 생성에서 todayProblem 캐시 정합성 이슈

> **카테고리**: 트러블슈팅  
> **날짜**: 2026-04-06  
> **관련 PR**: #214

---

## 한 줄 요약

`@Async + @Transactional(REQUIRES_NEW) + @Cacheable/@CacheEvict` 조합에서 조회/생성 타이밍이 엇갈리며 stale 캐시가 발생했다. 빈 결과 캐시 방지 + 캐시 키 정합성 보정 + 커밋 이후 무효화 전략으로 해결했다.

---

## 배경

- **기능**: 회원 선호도(카테고리) 추가 시 초기 문제를 비동기로 생성
- **조회 경로**: `DailyProblemService#getTodayProblem` (`todayProblem` 캐시 사용)
- **생성 경로**: `ProblemGenerator#generateInitialProblem` (`@Async`, `@Transactional(REQUIRES_NEW)`, `@CacheEvict`)

---

## 문제 / 목표

선호도 추가 직후 오늘의 문제 조회 시 빈 목록 또는 기존 목록이 반복 노출됨.  
특히 이미 1개 문제가 캐시에 있는 상태에서 새 문제가 생성되어야 할 때 1개가 계속 보이는 현상이 재현됨.

---

## 원인 분석

### 1) Negative Cache (빈 결과 캐시 고착)

`@Cacheable` 기본 동작으로 빈 리스트(`[]`)도 캐시됨 → 생성 전 조회가 빈값을 캐시에 고착

### 2) 캐시 키 정합성 불일치

조회 키(`memberId:date`)와 무효화 키가 불일치 → 무효화가 실제로 동작하지 않는 경우 발생

### 3) 비동기 + 별도 트랜잭션 타이밍 레이스

`generateInitialProblem`은 별도 스레드 + 별도 트랜잭션에서 수행. 생성 트랜잭션 커밋 전후로 조회가 들어오면 stale 데이터가 재캐싱될 수 있음.

**이미 1개 캐시가 있는 상태에서 stale non-empty 고착 케이스:**

```
1. todayProblem 캐시에 [1개] 존재
2. 카테고리 추가 → generateInitialProblem() 비동기 실행
3. 생성 완료 전 조회 → 캐시 hit → [1개] 응답
4. 캐시 무효화(evict) 발생
5. evict 직후 첫 조회가 커밋 가시화 이전 타이밍 → DB도 [1개]
6. [1개]가 다시 캐시 → stale non-empty 고착
```

**왜 `unless = isEmpty`만으로는 부족한가?**
- `unless = "#result == null || #result.isEmpty()"`는 empty 캐시만 차단
- stale `[1개]`는 non-empty → 정상 캐시 대상으로 판단 → 기존값 고착은 남음

---

## 해결 방향 비교

| 옵션 | 설명 | 채택 여부 |
|------|------|---------|
| **커밋 이후 무효화 보장** | 캐시 무효화 시점을 DB 커밋 이후로 정렬 → 레이스 윈도우 축소 | **채택** |
| `beforeInvocation = true` | 선제 무효화. 단, 생성 실패 시 공백 구간 확대 가능 | 미채택 |
| `@CachePut` 수동 재구성 | 최신값 즉시 반영. 단, 구현 복잡도 증가 | 미채택 |

---

## Before

```java
// DailyProblemService — 빈 결과도 캐시됨
@Cacheable(cacheNames = "todayProblem", key = "#memberId + ':' + T(java.time.LocalDate).now()")
public List<TodayProblemResponse> getTodayProblem(Long memberId) { ... }

// ProblemGenerator — 무효화 키 불일치 가능성 존재
@CacheEvict(cacheNames = "todayProblem", key = "#member.id + ':' + ...")
public void generateInitialProblem(Member member, ...) { ... }
```

---

## After

```java
// 1) 빈 결과 캐시 방지
@Cacheable(
    cacheNames = "todayProblem",
    key = "#memberId + ':' + T(java.time.LocalDate).now()",
    unless = "#result == null || #result.isEmpty()"  // 빈 결과는 저장하지 않음
)
public List<TodayProblemResponse> getTodayProblem(Long memberId) { ... }

// 2) 무효화 키 조회 키와 정확히 일치
@CacheEvict(cacheNames = "todayProblem", key = "#member.id + ':' + T(java.time.LocalDate).now()")
public void generateInitialProblem(Member member, ...) { ... }
```

**최종 운영 원칙:**
- 생성/할당 트랜잭션 커밋 이후 무효화가 보장되도록 설계
- 타이밍 레이스가 남는 구간은 폴링/재조회 전략으로 완화

---

## 결과

| 항목 | Before | After |
|------|--------|-------|
| 빈값 고착 (Negative Cache) | 발생 | 해소 |
| 캐시 키 정합성 | 불일치 가능 | 조회/무효화 키 일치 보장 |
| stale 재현 빈도 | 높음 | 감소 |

---

## 트레이드오프

- 빈 결과를 캐시하지 않으므로 생성 전 구간에서는 DB 조회가 증가할 수 있다.
- 강한 정합성을 위해서는 커밋 시점 이벤트 기반 무효화/재구성(`@CachePut`) 등 추가 설계가 필요하다. (개선 과제)

---

## 재발 방지 체크리스트

- [ ] 조회 키와 무효화 키가 동일 규칙인지 코드리뷰 시 확인
- [ ] `@Async + @Transactional + Cache` 결합 메서드는 타이밍 테스트 포함
- [ ] 빈 결과 캐시 여부를 기능 요구사항으로 명시
- [ ] 캐시 정책 변경 시 회귀 테스트(빈값/기존값/증가값 시나리오) 실행

---

## 배운 점 / 회고

- `@Cacheable`과 `@CacheEvict`의 키 식이 미묘하게 달라도 캐시가 기대대로 무효화되지 않는다. 키 계산 로직을 명확히 문서화하고 코드리뷰에서 반드시 확인해야 한다.
- Negative Cache(`[]` 캐시)는 예상보다 쉽게 발생한다. 빈 결과를 캐시할지 여부는 설계 초기에 결정해야 한다.
- 비동기 + 별도 트랜잭션 환경에서 캐시 정합성은 "캐시를 제거했다"보다 "DB가 이미 최신 상태인 시점에 캐시를 제거했다"가 중요하다. 트랜잭션 커밋 이후 무효화 원칙을 항상 지켜야 한다.
