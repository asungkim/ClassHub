# Feature: Teacher 회원가입 API

## 1. Problem Definition
- Auth PLAN 상 첫 구현 대상은 Teacher가 초대 없이도 플랫폼에 가입할 수 있는 self-onboarding 흐름이다.
- Member 엔티티와 Security 구성은 준비되었지만, 이메일/비밀번호 입력을 검증하고 Role=TEACHER로 계정을 생성하는 API가 없다.
- Teacher 가입 절차가 없으면 이후 Course/Notice 등 Teacher 중심 기능을 테스트할 계정을 만들 수 없다.

## 2. Requirements

### Functional
- Endpoint: `POST /auth/register/teacher`
  - Request Body: `{ "email": string, "password": string, "name": string }`
  - Response: `RsData<TeacherRegisterResponse>` (memberId, email, authority, createdAt, updatedAt). 토큰은 로그인 성공 시에만 발급한다.
- Validation
  - DTO에 `@Valid`, `@NotBlank`, `@Email`, `@Size` 등을 적용.
  - password는 정규식으로 8자 이상 + 숫자/영문/특수문자 조합을 요구.
  - name은 1~50자, 공백 trim.
- Flow
  1. DTO 검증 후 Service (AuthApplicationService) 호출.
  2. PasswordEncoder로 비밀번호 암호화.
  3. MemberRepository에 Role=TEACHER, isActive=true, teacherId=null 로 저장.
  4. 저장된 Member의 요약(멤버 ID, 이메일, Role=TEACHER, 생성/수정 시각)을 반환.
- 실패 시
  - 중복 이메일, Validation 실패: `BusinessException` + RsCode 매핑.
  - 저장 중 예외는 GlobalExceptionHandler 처리.

### Non-functional
- 모든 입력 DTO는 Bean Validation(`@Email`, `@NotBlank`, `@Size`) 적용.
- Service 계층에서 `@Transactional` 유지, 읽기/쓰기 구분.
- PasswordEncoder는 SecurityConfig에서 정의한 BCrypt Bean 사용.
- JWT 시크릿/만료 값은 `application.yml`의 `custom.jwt` 사용(테스트 시 override).
- Controller는 `RsData`로 응답, 로그에 email은 마스킹.
- 향후 rate-limit/bot 방지를 고려해 Hook 제공(별도 TODO).

## 3. API Design (Draft)
| Method | URL | Request | Response | Notes |
| --- | --- | --- | --- | --- |
| POST | `/auth/register/teacher` | `{ email, password, name }` | `RsData<{ memberId, email, authority, createdAt, updatedAt }>` | Role은 항상 TEACHER, 토큰은 로그인 시 발급 |

HTTP Status는 ResponseAspect가 RsCode에 따라 설정(성공=200, 실패=4xx).

## 4. Domain Model (Draft)
- DTO
  - `TeacherRegisterRequest(email, password, name)` – Bean Validation 어노테이션 포함.
  - `TeacherRegisterResponse(memberId, email, authority, createdAt, updatedAt)`
- Service
  - `AuthApplicationService.registerTeacher(request)`
    - 이메일 중복 검사(`MemberRepository.existsByEmail`).
    - 패스워드 암호화 후 Member 생성.
    - JwtProvider로 토큰 발급(옵션).
- Repository: 기존 `MemberRepository`.
- Mapper: 도메인 객체 → 응답 DTO 변환 유틸.

## 5. TDD Plan
1. **Controller 단위/통합 테스트**
   - 정상 요청 → 200 + Role=TEACHER + 토큰 존재.
   - 잘못된 입력(email 형식, password 길이) → 400 + Validation 메시지.
2. **Service 단위 테스트**
   - 중복 이메일 시 예외 발생 확인.
   - PasswordEncoder가 호출되어 암호화 결과가 raw와 다름을 검증.
3. **Repository/데이터 상태 검증**
   - 저장된 Member가 Role=TEACHER, isActive=true, teacherId=null 인지 확인.
4. **응답 DTO 테스트**
   - 반환된 TeacherRegisterResponse에 memberId/email/authority/createdAt/updatedAt이 포함되는지 검증.
