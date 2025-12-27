# Password Reset Plan (비밀번호 찾기)

## 1. 기능 개요

사용자가 비밀번호를 잊어버렸을 때, 이메일 + 핸드폰 번호 본인 확인 후 즉시 새 비밀번호를 설정할 수 있는 경량 비밀번호 재설정 기능.

### 요구사항

- **이메일 발송 불필요**: SMTP 인프라 없이 작동
- **2단계 인증**: 이메일 + 핸드폰 번호로 본인 확인
- **보안**: 비밀번호 평문 저장 없이 BCrypt 해싱 유지
- **즉시 재설정**: 검증 성공 시 바로 새 비밀번호 입력 가능

### 플로우

```
1. 사용자: 이메일 + 핸드폰 번호 입력
2. 서버: DB에서 일치하는 Member 조회
3. ✅ 일치하면 → 임시 토큰 발급 (5분 유효)
4. 사용자: 임시 토큰 + 새 비밀번호 입력
5. 서버: 토큰 검증 후 비밀번호 업데이트 (BCrypt)
6. 완료!
```

---

## 2. API 설계

### 2.1. POST /api/v1/auth/password-reset/verify

**요청**: 이메일 + 핸드폰 번호로 본인 확인

```json
{
  "email": "student@example.com",
  "phoneNumber": "010-1234-5678"
}
```

**응답 (성공)**: 임시 리셋 토큰 발급

```json
{
  "code": 1000,
  "message": "본인 확인 완료",
  "data": {
    "resetToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 300
  }
}
```

**응답 (실패)**: 정보 불일치

```json
{
  "code": 4004,
  "message": "일치하는 회원 정보를 찾을 수 없습니다.",
  "data": null
}
```

### 2.2. POST /api/v1/auth/password-reset/complete

**요청**: 임시 토큰 + 새 비밀번호

```json
{
  "resetToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "newPassword": "NewSecurePassword123!"
}
```

**응답 (성공)**:

```json
{
  "code": 1000,
  "message": "비밀번호가 재설정되었습니다.",
  "data": null
}
```

**응답 (실패 - 토큰 만료)**:

```json
{
  "code": 4010,
  "message": "비밀번호 재설정 시간이 만료되었습니다. 다시 시도해주세요.",
  "data": null
}
```

---

## 3. 도메인 모델

### 3.1. PasswordResetToken (인메모리 저장)

DB 테이블 생성 대신 간단한 인메모리 저장소 사용 (RefreshTokenStore와 유사).

```java
public class PasswordResetToken {
    private String token;        // UUID
    private String memberId;     // Member UUID
    private LocalDateTime expiresAt;
}
```

**저장소**: `ConcurrentHashMap<String, PasswordResetToken>`

**만료 시간**: 5분

---

## 4. 구현 계획 (TDD)

### Phase 1: Repository Layer (이미 완료)

- ✅ `MemberRepository.findByEmailAndPhoneNumber(email, phoneNumber)` - 기존 메서드 활용

### Phase 2: Service Layer

**클래스**: `PasswordResetService`

**메서드**:
1. `verifyMemberAndIssueToken(email, phoneNumber)` → `PasswordResetTokenResponse`
   - Member 조회 (이메일 + 핸드폰)
   - 없으면 예외: `MemberNotFoundException`
   - 임시 토큰 생성 (UUID)
   - 토큰 저장 (5분 TTL)
   - 응답 반환

2. `resetPassword(resetToken, newPassword)` → `void`
   - 토큰 검증 (존재 여부 + 만료 여부)
   - 없거나 만료 시 예외: `InvalidResetTokenException`
   - Member 조회
   - 비밀번호 BCrypt 인코딩
   - `member.changePassword(encodedPassword)` 호출
   - 토큰 삭제 (일회용)

**TDD 순서**:
1. RED: `shouldIssueResetToken_whenValidEmailAndPhone()` 실패 테스트 작성
2. GREEN: 최소 구현 (토큰 생성 로직)
3. REFACTOR: 토큰 저장소 분리

4. RED: `shouldThrowException_whenMemberNotFound()` 실패 테스트
5. GREEN: 예외 처리 구현

6. RED: `shouldResetPassword_whenValidToken()` 실패 테스트
7. GREEN: 비밀번호 재설정 로직 구현

8. RED: `shouldThrowException_whenTokenExpired()` 실패 테스트
9. GREEN: 만료 검증 로직 추가

### Phase 3: Controller Layer

**클래스**: `PasswordResetController`

**엔드포인트**:
1. `POST /api/v1/auth/password-reset/verify`
2. `POST /api/v1/auth/password-reset/complete`

**TDD 순서**:
1. RED: `/verify` 성공 케이스 MockMvc 테스트
2. GREEN: 컨트롤러 구현
3. RED: `/verify` 실패 케이스 테스트 (404)
4. GREEN: 예외 처리 확인

5. RED: `/complete` 성공 케이스 테스트
6. GREEN: 컨트롤러 구현
7. RED: `/complete` 토큰 만료 케이스 테스트 (401)
8. GREEN: 예외 처리 확인

### Phase 4: 통합 테스트 (Optional)

- `@SpringBootTest`로 전체 플로우 검증
- verify → complete 순차 호출 시나리오

---

## 5. DTO 설계

### Request DTO

```java
// PasswordResetVerifyRequest.java
public record PasswordResetVerifyRequest(
    @NotBlank @Email String email,
    @NotBlank String phoneNumber
) {}

// PasswordResetCompleteRequest.java
public record PasswordResetCompleteRequest(
    @NotBlank String resetToken,
    @NotBlank @Size(min = 8) String newPassword
) {}
```

### Response DTO

```java
// PasswordResetTokenResponse.java
public record PasswordResetTokenResponse(
    String resetToken,
    int expiresIn  // seconds (300)
) {}
```

---

## 6. 보안 고려사항

### 6.1. 타이밍 공격 방지

- 회원 존재 여부를 노출하지 않기 위해, 이메일/핸드폰 불일치 시에도 동일한 응답 시간 유지
- 현재는 간단히 "일치하는 정보 없음" 메시지 반환 (학원 내부 시스템이므로 타협 가능)

### 6.2. 토큰 일회용 처리

- 비밀번호 재설정 성공 시 토큰 즉시 삭제
- 동일 토큰으로 재시도 불가

### 6.3. Rate Limiting (향후)

- 동일 이메일로 5분 내 3회 이상 요청 시 차단 (향후 추가)

### 6.4. 비밀번호 정책

- 최소 8자 이상
- 현재 비밀번호와 동일 여부 검증 (Optional)

---

## 7. 패키지 구조

```
domain/auth/
├── web/
│   └── PasswordResetController.java
├── application/
│   └── PasswordResetService.java
├── dto/
│   ├── request/
│   │   ├── PasswordResetVerifyRequest.java
│   │   └── PasswordResetCompleteRequest.java
│   └── response/
│       └── PasswordResetTokenResponse.java
├── token/
│   ├── PasswordResetToken.java
│   └── PasswordResetTokenStore.java
└── exception/
    └── InvalidResetTokenException.java (global/exception에 추가 가능)
```

---

## 8. 테스트 전략

### Repository
- ✅ 기존 `MemberRepository.findByEmailAndPhoneNumber()` 사용
- 추가 테스트 불필요 (이미 존재한다고 가정)

### Service
- Mock Repository로 단위 테스트
- 토큰 생성/검증/만료 시나리오 검증

### Controller
- MockMvc + MockBean Service
- 요청/응답 JSON 검증
- 예외 케이스 (404, 401) 검증

---

## 9. 구현 순서 (TDD)

1. **PasswordResetToken + Store 클래스 작성** (테스트 없이 단순 모델)
2. **PasswordResetService 테스트 작성** (RED)
3. **PasswordResetService 구현** (GREEN)
4. **리팩터링** (토큰 만료 로직 분리)
5. **PasswordResetController 테스트 작성** (RED)
6. **PasswordResetController 구현** (GREEN)
7. **전체 빌드 및 수동 테스트** (Postman/Swagger)

---

## 10. 완료 조건

- [ ] 모든 테스트 통과 (`./gradlew test`)
- [ ] Swagger UI에서 `/password-reset/verify`, `/password-reset/complete` 확인 가능
- [ ] 이메일 + 핸드폰 불일치 시 404 응답
- [ ] 토큰 만료 시 401 응답
- [ ] 비밀번호 재설정 후 로그인 성공
- [ ] AGENT_LOG에 구현 이벤트 기록
