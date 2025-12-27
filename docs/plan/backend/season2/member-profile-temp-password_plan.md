# Feature: 내 정보/임시 비밀번호

## 1. Problem Definition
- 로그인 사용자가 자신의 정보를 확인하고 수정할 수 있어야 한다.
- 이메일/휴대폰으로 본인 확인 후 임시 비밀번호를 즉시 발급해 빠르게 복구할 수 있어야 한다.
- Student는 Member 정보뿐 아니라 StudentInfo도 함께 다뤄야 한다.

## 2. Requirements
### Functional
- 내 정보 조회
  - Teacher/Assistant: Member 정보만 반환
  - Student: Member + StudentInfo를 함께 반환
- 내 정보 수정
  - Member 기본 필드(이메일/이름/전화번호/비밀번호) 수정 가능
  - Student는 StudentInfo(학교/학년/생년월일/보호자 연락처) 수정 가능
  - 비밀번호는 BCrypt 인코딩으로 저장
  - 이메일 변경 시 중복 검증
- 임시 비밀번호 즉시 발급
  - 이메일 + 휴대폰 번호 입력으로 본인 확인
  - 검증 성공 시 `Classmate` + 4자리 숫자 + `!` 패턴의 임시 비밀번호를 즉시 발급하고 응답에 포함
  - 발급된 비밀번호는 BCrypt로 저장
  - 화면에 `임시 비밀번호: Classmate1234!` 형태로 노출

### Non-functional
- SMTP/메일 인프라 없이 동작
- 기존 RsData/RsCode 규약 유지
- 실패 시 일관된 예외 코드 반환

## 3. API Design (Draft)
- `GET /api/v1/members/me`
  - Response: `{ member: { id, email, name, phoneNumber, role }, studentInfo?: {...} }`
  - 비밀번호는 응답에 포함하지 않음
- `PUT /api/v1/members/me`
  - Request: `{ email?, name?, phoneNumber?, password?, studentInfo? }`
  - Response: updated profile (GET과 동일 스키마)
- `POST /api/v1/auth/temp-password`
  - Request: `{ email, phoneNumber }`
  - Response: `{ tempPassword: "Classmate1234!" }`

## 4. Domain Model (Draft)
- Entity
  - Member, StudentInfo 기존 엔티티 재사용
- DTO
  - MemberProfileResponse (Member + optional StudentInfo)
  - MemberProfileUpdateRequest (Member 필드 + optional StudentInfo)
  - TempPasswordRequest / TempPasswordResponse
- Service
  - MemberProfileService: 조회/수정 로직
  - TempPasswordService (또는 AuthService 확장): 본인 확인 + 비밀번호 갱신

## 5. TDD Plan
1. MemberProfileService
   - `shouldReturnMemberProfile_whenTeacherOrAssistant`
   - `shouldReturnMemberAndStudentInfo_whenStudent`
   - `shouldUpdateMemberFields_whenValidRequest`
   - `shouldEncodePassword_whenPasswordProvided`
   - `shouldUpdateStudentInfo_whenStudent`
   - `shouldRejectDuplicateEmail_whenEmailChanges`
2. TempPasswordService/AuthService
   - `shouldIssueTempPassword_whenEmailAndPhoneMatch`
   - `shouldUpdatePasswordWithBCrypt_whenTempPasswordIssued`
   - `shouldThrowNotFound_whenEmailOrPhoneMismatch`
3. Controller
   - `GET /members/me` success (teacher/assistant/student)
   - `PUT /members/me` success + validation 실패
   - `POST /auth/temp-password` success + mismatch 실패
