# 003. `@Lob` + `String`이 `LONGTEXT` 대신 `TINYTEXT`로 매핑되는 문제

> **카테고리**: 트러블슈팅  
> **날짜**: 2026-04-06  
> **관련 PR**: #208, #211  
> **관련 엔티티**: `Problem.aiAnswer`, `SubmissionFeedback.strengths`, `weaknesses`, `suggestion`

---

## 배경

AI 답안(`aiAnswer`)과 피드백 필드에 대용량 텍스트를 저장하기 위해 `@Lob` 어노테이션을 붙였다. DDL 로그를 확인하니 MySQL 컬럼 타입이 `LONGTEXT`가 아닌 **`TINYTEXT`(최대 255바이트)** 로 생성되고 있었다.

---

## 문제 / 목표

- `@Lob` + `String` 조합이 왜 `TINYTEXT`로 매핑되는지 원인 파악
- 길이 제한 없이 대용량 텍스트를 저장할 수 있는 올바른 방법으로 수정

---

## Before

```java
// Problem.java
@Lob
@Column(nullable = false)
private String aiAnswer;

// SubmissionFeedback.java
@Lob
@Column(nullable = false)
private String strengths;
```

DDL 결과 → `TINYTEXT` (최대 255바이트)

---

## 원인 분석

### 1단계 — Dialect 설정 문제 의심

`application-*.yml`에 Hibernate dialect가 다음과 같이 설정되어 있었다.

```yaml
properties:
  hibernate:
    dialect: org.hibernate.dialect.MySQLDialect
```

| Dialect | 대상 | `@Lob` + `String` 매핑 |
|---------|------|----------------------|
| `MySQLDialect` (generic) | 버전 무관 | `TINYTEXT` |
| `MySQL5Dialect` | MySQL 5.x | `LONGTEXT` |
| `MySQL8Dialect` | MySQL 8.x | `LONGTEXT` |
| 미지정 (자동 감지) | JDBC 드라이버로 판단 | `LONGTEXT` |

dialect 설정을 제거(자동 감지)해도 여전히 `TINYTEXT`가 생성되었다.

### 2단계 — Hibernate 6 내부 타입 매핑이 실제 원인

```
@Lob + String → JDBC CLOB → MySQLDialect.columnType(CLOB) = "tinytext"
```

Hibernate 6에서 자동 감지로 선택된 dialect도 내부적으로 `MySQLDialect`를 상속하므로 동일한 문제가 발생한다. **dialect 설정이 아니라 엔티티의 컬럼 정의 자체를 바꿔야 한다.**

---

## After

```java
// Problem.java
@Column(nullable = false, columnDefinition = "LONGTEXT")
private String aiAnswer;

// SubmissionFeedback.java
@Column(nullable = false, columnDefinition = "LONGTEXT")
private String strengths;

@Column(nullable = false, columnDefinition = "LONGTEXT")
private String weaknesses;

@Column(nullable = false, columnDefinition = "LONGTEXT")
private String suggestion;
```

`@Lob`을 제거하고 `columnDefinition = "LONGTEXT"`로 DDL에 직접 명시하여 Hibernate 타입 매핑을 우회한다.

`application-*.yml`의 `dialect: org.hibernate.dialect.MySQLDialect`도 함께 제거 (자동 감지 사용).

---

## 결과

| 항목 | Before | After |
|------|--------|-------|
| `aiAnswer` 컬럼 타입 | `TINYTEXT` (255 bytes) | `LONGTEXT` (4GB) |
| 어노테이션 | `@Lob` + `@Column` | `@Column(columnDefinition = "LONGTEXT")` |
| dialect 설정 | `MySQLDialect` 명시 | 자동 감지 |

---

## 배운 점 / 회고

- `@Lob`이 "무조건 대용량 컬럼"을 보장하지 않는다. Hibernate dialect 구현체마다 매핑 결과가 다르다.
- Hibernate 6(Spring Boot 3.x)에서는 dialect를 명시하지 않는 것이 더 안전하다. 자동 감지가 권장 방향이다.
- DB 종속 컬럼 타입이 필요할 때는 `columnDefinition`으로 DDL을 직접 제어하는 방식이 가장 예측 가능하다.
- DDL 로그를 주기적으로 확인하는 습관이 중요하다. 의도와 다른 타입이 생성되어도 런타임에서는 에러 없이 동작하다가 데이터 저장 시점에서야 문제가 드러난다.
