# HaruHana API 레퍼런스

## 공통 사항

### Base URL
```
/v1
```

### 인증
- JWT Bearer 토큰 사용
- `Authorization: Bearer {accessToken}` 헤더 필요 (인증 필요 API)

### 공통 응답 형식

**성공**
```json
{
  "status": "SUCCESS",
  "data": { ... }
}
```

**실패**
```json
{
  "status": "ERROR",
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

### 엔드포인트 목록

| 도메인 | 인증 필요 | Base Path |
|--------|----------|-----------|
| 인증 | 일부 | `/v1/auth` |
| 회원 | 일부 | `/v1/members` |
| 카테고리 | No | `/v1/categories` |
| 오늘의 문제 | Yes | `/v1/daily-problem` |
| 스트릭 | Yes | `/v1/streaks` |
| 파일 업로드 | Yes | `/v1/storage` |
| 관리자 | Yes (ADMIN) | `/v1/admin` |

---

## 인증 (Auth)

### 로그인

```
POST /v1/auth/login
```

**Request Body**
```json
{
  "loginId": "user01",
  "password": "password123"
}
```

**Response Body**
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi..."
}
```

---

### 토큰 재발급

```
POST /v1/auth/reissue
```

**Request Body**
```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

**Response Body**
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi..."
}
```

---

### 로그아웃

```
POST /v1/auth/logout
Authorization: Bearer {accessToken}
```

**Response**: 200 OK (data: null)

---

## 회원 (Member)

### 회원가입

```
POST /v1/members/sign-up
```

**Request Body**
```json
{
  "loginId": "user01",
  "password": "password123",
  "nickname": "하루하나"
}
```

**Response**: 201 Created (data: null)

---

### 닉네임 중복 확인

```
GET /v1/members/nickname?nickname={nickname}
```

**Response Body**
```json
{
  "isDuplicated": false
}
```

---

### 로그인 ID 중복 확인

```
GET /v1/members/login-id?loginId={loginId}
```

**Response Body**
```json
{
  "isDuplicated": false
}
```

---

### 내 프로필 조회

```
GET /v1/members
Authorization: Bearer {accessToken}
```

**Response Body**
```json
{
  "id": 1,
  "nickname": "하루하나",
  "profileImageUrl": "https://...",
  "memberPreferences": [
    {
      "preferenceId": 1,
      "categoryTopicName": "Spring",
      "difficulty": "EASY"
    },
    {
      "preferenceId": 2,
      "categoryTopicName": "Java",
      "difficulty": "MEDIUM"
    }
  ]
}
```

---

### 프로필 수정

```
PATCH /v1/members
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "nickname": "새닉네임",
  "objectKey": "profile/uuid.jpg"
}
```

> `nickname`, `objectKey` 중 변경할 항목만 포함 가능

**Response**: 200 OK (data: null)

---

### 학습 설정 추가

```
POST /v1/members/preferences
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "categoryTopicId": 5,
  "difficulty": "MEDIUM"
}
```

**Response**: 204 No Content

> 최대 5개까지 추가 가능. 동일한 카테고리 토픽 + 난이도 조합 중복 불가.

---

### 학습 설정 수정

```
PATCH /v1/members/preferences/{preferenceId}
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "categoryTopicId": 5,
  "difficulty": "HARD"
}
```

**Response**: 200 OK (data: null)

> 변경사항은 다음날 자정부터 적용. 당일 중복 변경 시 기존 레코드 업데이트.

---

### 회원 탈퇴

```
DELETE /v1/members
Authorization: Bearer {accessToken}
```

**Response**: 200 OK (data: null)

> 탈퇴 시 loginId와 nickname은 즉시 익명화됨.

---

### 디바이스 토큰 등록/갱신

```
PATCH /v1/members/devices
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "deviceToken": "fcm-device-token-here"
}
```

**Response**: 200 OK (data: null)

---

### 디바이스 토큰 삭제

```
DELETE /v1/members/devices
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "deviceToken": "fcm-device-token-here"
}
```

**Response**: 200 OK (data: null)

---

## 카테고리 (Category)

### 카테고리 전체 조회

```
GET /v1/categories
```

**Response Body**
```json
[
  {
    "id": 1,
    "name": "개발",
    "groups": [
      {
        "id": 1,
        "name": "백엔드",
        "topics": [
          { "id": 1, "name": "Spring" },
          { "id": 2, "name": "Java" }
        ]
      }
    ]
  }
]
```

---

## 오늘의 문제 (Daily Problem)

### 오늘의 문제 조회

```
GET /v1/daily-problem/today
Authorization: Bearer {accessToken}
```

**Response Body**
```json
{
  "dailyProblemId": 10,
  "assignedAt": "2026-02-21",
  "isSolved": false,
  "problem": {
    "id": 5,
    "title": "Spring Bean의 생명주기에 대해 설명하시오.",
    "difficulty": "MEDIUM",
    "categoryTopicName": "Spring"
  }
}
```

---

### 날짜로 문제 조회

```
GET /v1/daily-problem?date={yyyy-MM-dd}
Authorization: Bearer {accessToken}
```

**Response Body**: 오늘의 문제 조회와 동일

---

### 문제 상세 조회

```
GET /v1/daily-problem/{dailyProblemId}
Authorization: Bearer {accessToken}
```

**Response Body**
```json
{
  "dailyProblemId": 10,
  "assignedAt": "2026-02-21",
  "isSolved": true,
  "problem": {
    "id": 5,
    "title": "Spring Bean의 생명주기에 대해 설명하시오.",
    "description": "Spring IoC 컨테이너에서 Bean이...",
    "difficulty": "MEDIUM",
    "categoryTopicName": "Spring"
  },
  "submission": {
    "answer": "Spring Bean은 ...",
    "aiAnswer": "Spring Bean의 생명주기는 ...",
    "submittedAt": "2026-02-21T14:30:00",
    "isOnTime": true
  }
}
```

> `submission` 필드는 풀이 제출 후에만 포함

---

### 풀이 제출

```
POST /v1/daily-problem/{dailyProblemId}/submissions
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "answer": "Spring Bean의 생명주기는 다음과 같습니다..."
}
```

**Response Body**
```json
{
  "submissionId": 1,
  "aiAnswer": "Spring Bean의 생명주기는 ...",
  "isOnTime": true
}
```

> 제시간 제출(isOnTime=true)이면 스트릭 카운트에 반영

---

## 스트릭 (Streak)

### 스트릭 조회

```
GET /v1/streaks
Authorization: Bearer {accessToken}
```

**Response Body**
```json
{
  "currentStreak": 7,
  "maxStreak": 30
}
```

---

## 파일 업로드 (Storage)

### Presigned URL 발급

```
POST /v1/storage/presigned-url
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "uploadType": "PROFILE_IMAGE"
}
```

**Response Body**
```json
{
  "presignedUrl": "https://s3.amazonaws.com/bucket/...",
  "objectKey": "profile/uuid-here.jpg"
}
```

> 클라이언트는 반환된 `presignedUrl`로 직접 PUT 요청하여 파일 업로드

---

### 업로드 완료 처리

```
POST /v1/storage/upload-complete
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "objectKey": "profile/uuid-here.jpg"
}
```

**Response**: 200 OK (data: null)

> 이후 `PATCH /v1/members`로 프로필 이미지 objectKey를 업데이트해야 적용됨

---

## 관리자 API

> 모든 관리자 API는 `ROLE_ADMIN` 권한 필요

자세한 내용은 [ADMIN.md](./ADMIN.md) 참고



### 엔드포인트 요약

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/v1/admin/statistics` | 서비스 통계 조회 |
| GET | `/v1/admin/members` | 회원 목록 조회 (페이징) |
| GET | `/v1/admin/members/{id}/preferences` | 회원 선호 설정 조회 |
| PATCH | `/v1/admin/members/{id}/nicknames` | 회원 닉네임 수정 |
| PATCH | `/v1/admin/members/{id}/roles` | 회원 권한 변경 |
| DELETE | `/v1/admin/members/{id}` | 회원 삭제 |
| GET | `/v1/admin/problems` | 문제 목록 조회 (페이징) |
| POST | `/v1/admin/categories` | 카테고리 생성 |
| PATCH | `/v1/admin/categories/{id}` | 카테고리 수정 |
| DELETE | `/v1/admin/categories/{id}` | 카테고리 삭제 |
| POST | `/v1/admin/categories/groups` | 카테고리 그룹 생성 |
| PATCH | `/v1/admin/categories/groups/{id}` | 카테고리 그룹 수정 |
| DELETE | `/v1/admin/categories/groups/{id}` | 카테고리 그룹 삭제 |
| POST | `/v1/admin/categories/topics` | 카테고리 토픽 생성 |
| PATCH | `/v1/admin/categories/topics/{id}` | 카테고리 토픽 수정 |
| DELETE | `/v1/admin/categories/topics/{id}` | 카테고리 토픽 삭제 |

---

## 헬스체크 & 모니터링

| Endpoint | 인증 | 설명 |
|----------|------|------|
| `GET /actuator/health` | No | 애플리케이션 상태 확인 |
| `GET /actuator/info` | No | 애플리케이션 정보 |
| `GET /actuator/prometheus` | No | Prometheus 메트릭 |
