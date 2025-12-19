# Feature: Member Registration (Teacher & Student)

## 1. Problem Definition
- 현재 RegisterService는 Teacher 가입만 처리하고 `/api/v1/members/register/teacher`만 존재한다. 학생은 Requirement v1.3 기준으로 자유 가입이 가능해야 하지만 API/도메인 구현이 전무하다.
- RegisterTeacherRequest처럼 역할별 DTO가 분리돼 있어 공통 필드를 중복 선언해야 하고, 추후 조교/학생 흐름을 추가하기 어렵다. 또한 `docs/design/final-entity-spec.md`에서 규정한 StudentInfo 구조/Enum을 반영하지 못하고 있다.
- 학생 가입 시 Member 기본 정보 + StudentInfo(학교, 학년, 생년월일, 학부모 연락처)를 한 번에 저장해야 하는데, StudentInfo 엔티티/레포지토리가 아직 정의되지 않았다.

## 2. Requirements

### Functional
1. **공통 Request/Service 구조**
   - RegisterTeacherRequest를 `RegisterMemberRequest`로 rename하고 `email/password/name/phoneNumber` 공통 필드를 가진다.
   - 학생 전용 추가 필드를 위해 `RegisterStudentRequest`는 `RegisterMemberRequest`를 상속/조합해 `schoolName`, `grade`, `birthDate`, `parentPhone`를 포함한다.
2. **엔드포인트**
   - Teacher: `POST /api/v1/members/register/teacher` (현행 유지, 새 Request 타입 반영).
   - Student: `POST /api/v1/members/register/student`.
3. **도메인 저장**
   - 학생 가입 시 Member(role=STUDENT)를 생성하고, 동일 트랜잭션에서 StudentInfo(memberId FK) 레코드를 생성한다.
   - StudentInfo.grade는 `StudentGrade` Enum(E1~E6, M1~M3, H1~H3, GAP_YEAR 등 spec 정의)만 허용.
   - Parent phone과 schoolName도 Normalizer/Validator를 적용(숫자/`-`, trim, upper/lower rule 등).
4. **응답**
   - 두 엔드포인트 모두 Access/Refresh 토큰을 발급하고 `LoginResponse` + Refresh HttpOnly 쿠키를 반환.
5. **예외 처리**
   - 이메일 중복: `RsCode.DUPLICATE_EMAIL`.
   - Soft-delete 회원 재가입: `RsCode.MEMBER_INACTIVE`.
   - StudentInfo 누락 및 grade/birthDate 형식 오류: `RsCode.BAD_REQUEST`.

### Non-functional
- 공통 RegisterService를 통해 역할별 추가 로직만 분기하고, 비밀번호/이메일/전화번호 정규화는 재사용한다.
- StudentInfo 생성 실패 시 Member 생성도 롤백되도록 동일 트랜잭션 유지.
- Enum/Validator와 Normalizer 로직은 향후 Assistant/TeacherInfo 추가 시 재사용할 수 있도록 support 패키지로 모듈화.

## 3. API Design (Draft)

| Method | URL                                  | Request                                                                                                                                             | Response/Status                                                                                                                          | Error Codes                                                                                  |
| ------ | ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| POST   | `/api/v1/members/register/teacher`   | ```json<br>{ "email": "teacher@classhub.com", "password": "Classhub!1", "name": "김선생", "phoneNumber": "010-1234-5678" }```                     | `200 OK` + `RsData<LoginResponse>` (memberId, accessToken, accessTokenExpiresAt); Refresh 쿠키 세팅                                       | `DUPLICATE_EMAIL`, `INVALID_PHONE_FORMAT`, `WEAK_PASSWORD`, `MEMBER_INACTIVE`, `BAD_REQUEST` |
| POST   | `/api/v1/members/register/student`   | ```json<br>{ "email": "student@classhub.com", "password": "Classhub!1", "name": "홍길동", "phoneNumber": "010-2222-3333", "schoolName": "서울중", "grade": "M2", "birthDate": "2010-03-15", "parentPhone": "010-9999-8888" }``` | `200 OK` + `RsData<LoginResponse>` (memberId, accessToken, accessTokenExpiresAt); Refresh 쿠키 세팅                                       | `DUPLICATE_EMAIL`, `INVALID_PHONE_FORMAT`, `INVALID_STUDENT_INFO`, `BAD_REQUEST`            |

## 4. Domain Model (Draft)
- **Member**: 기존 필드 유지, role=TEACHER/STUDENT 분기만 확대.
- **StudentInfo (신규 엔티티)**  
  - `@OneToOne` with Member (memberId unique).  
  - Fields: `schoolName`(trim, 최대 60자), `grade`(StudentGrade enum), `birthDate`(`LocalDate`), `parentPhone`(Normalizer).  
  - Repository: `StudentInfoRepository` (`findByMemberId`, `existsByParentPhoneAndMemberIdNot` 등 확장 대비).
- **StudentGrade Enum**: Spec v1.3 정의(E1~E6, M1~M3, H1~H3, GAP_YEAR).
- **RegisterMemberRequest**: 공통 DTO(record) + `normalizedEmail`, `normalizedPhoneNumber`.
- **RegisterStudentRequest**: `RegisterMemberRequest` 필드 포함 + StudentInfo 필드 + Normalizer/Validator (`@Pattern`, `@Past`).
- **RegisterService**  
  - `registerTeacher(RegisterMemberRequest request)`  
  - `registerStudent(RegisterStudentRequest request)` → Member 생성 후 StudentInfoRepository.save.

## 5. TDD Plan
1. **Repository/Entity**
   - `StudentInfoRepositoryTest`: 저장/조회, memberId unique constraint, parentPhone normalization 확인.
   - MemberRepositoryTest는 이미 existsByEmail 검증이 있으므로 필요 시 parentPhone 중복 로직 추가.
2. **RegisterServiceTest**
   - `shouldRegisterStudent_whenInputValid`: Member + StudentInfo 저장, AuthService.login 호출 검증.
   - `shouldFailStudent_whenDuplicateEmail`: 기존 로직 재사용.
   - `shouldNormalizeStudentInfo`: schoolName trim, parentPhone normalizer, grade enum validation.
3. **Controller 테스트**
   - `MemberControllerTest.registerTeacher_shouldReturnTokensAndSetCookie` (기존 API) → 새 Request로 업데이트.
   - `MemberControllerTest.registerStudent_shouldReturnTokensAndSetCookie`: 성공/Validation 실패 케이스(MockMvc + Json).
4. **회귀**
   - `/api/v1/members/register/*` 경로 permitAll 여부를 SecurityIntegrationTest에서 확인.
   - `./gradlew test` 전체 실행으로 Auth/Member 관련 회귀 검증.

## 6. Implementation Steps (3단계)
1. **도메인 기초 구성**
   - `RegisterTeacherRequest`를 `RegisterMemberRequest`로 rename하고 공통 Normalizer/Validator를 적용한다.  
   - `docs/design/final-entity-spec.md`에 정의된 대로 `StudentGrade` Enum, `StudentInfo` 엔티티/Repository, `RegisterStudentRequest`(StudentInfo 필드 포함)를 생성한다.
2. **RegisterService 확장**
   - `registerTeacher`를 새 Request 사용으로 리팩터링하고, `registerStudent` 메서드를 추가해 Member + StudentInfo 생성/토큰 발급까지 구현한다.  
   - RegisterServiceTest로 Teacher/Student 경로를 모두 TDD로 검증한다.
3. **API 계층 정비**
   - MemberController에 `/register/student` 엔드포인트를 추가하고 MockMvc 테스트로 성공/Validation 실패/Refresh 쿠키 동작을 검증한다.  
   - SecurityConfig/JwtFilter에서 `/api/v1/members/register/*`를 permitAll로 등록하고, 전체 `./gradlew test`를 실행해 회귀를 확인한다.
