# Feature: 조교 및 학생 관리 (목록/비활성화/CRUD) 백엔드 연동

## 1. Problem Definition

- 프런트에서 조교(Member role=ASSISTANT)와 학생(StudentProfile) 목록을 보거나 비활성화/CRUD 할 백엔드 API가 부족하다.
- Teacher는 자신의 조교/학생을 모두 관리해야 하고, Assistant는 학생 관리만 가능해야 한다.
- 조교 비활성화(로그인 불가 처리)와 역할 기반 접근 제어가 명확히 정의되어야 한다.
- 학생 관리는 Member가 아니라 **StudentProfile** 기준으로만 이뤄져야 하며, 학생 CRUD/퇴원은 프로필 엔티티를 통해 수행한다.

## 2. Requirements

### Functional

- **조교 목록**: Teacher 전용 `GET /api/v1/members?role=ASSISTANT` (필터: 이름 부분 일치, active 여부). 응답에 id/name/email/role/active/createdAt 포함.
- **조교 비활성화**: Teacher 전용 `PATCH /api/v1/members/{memberId}/deactivate`로 active=false 처리 후 Refresh 토큰 무효화. 이미 비활성 상태면 멱등 처리. 비활성 상태일 경우 앞으로 로그인 못하게 해야됨.
- **학생 목록**: 기존 `GET /api/v1/student-profiles`에 courseId/name/active 필터 지원(필요 시 active 필터 추가). Teacher/Assistant 모두 사용 가능하되 소유권 체크. 학생 관리는 언제나 StudentProfile 기준이다.
- **학생 상세/수정/생성/퇴원**: 기존 `GET/POST/PATCH/DELETE /api/v1/student-profiles/{id}` 사용하되 **Teacher만** 생성/수정/퇴원(삭제) 가능하도록 한다. DELETE는 퇴원(soft deactivate)로 동작하고 소유권/역할 검증. Assistant는 목록/상세 조회만 허용. 퇴원 시 해당 학생 Member.active를 false로 비활성화해 로그인 차단한다(StudentProfile은 소유 상태만 갱신).
- **권한/소유권**: Teacher는 본인이 소유한 조교/학생만, Assistant는 자신이 소속된 Teacher의 학생(StudentProfile)을 **조회만** 가능(생성/수정/퇴원 불가).

### Non-functional

- **응답 규격**: `RsData`/`RsCode` + `ResponseAspect`로 HTTP status 매핑.
- **도메인 규칙**: Member 엔티티 active 플래그 사용(없다면 추가) 및 글로벌 auditing 유지. 비활성화 시 재로그인 차단, 기존 Access는 만료 흐름 안내.
- **검증/예외**: 존재하지 않음(404), 권한 없음(403), 이미 비활성(200 멱등) 코드 정의. 입력 검증은 `jakarta.validation`.
- **성능/정렬**: 기본 정렬 생성일 내림차순. pageable 사용 시 PageResponse 유지.
- **문서화**: SpringDoc에 새로운 Member 목록/비활성화 API 스펙 추가, OpenAPI 갱신 후 프런트 타입 재생성.

## 3. API Design (Draft)

| Method | URL                                     | Request                                                      | Response                                  | Notes                          |
| ------ | --------------------------------------- | ------------------------------------------------------------ | ----------------------------------------- | ------------------------------ |
| GET    | `/api/v1/members`                       | `role=ASSISTANT` (required), `name?`, `active?`, `pageable?` | `RsDataList<MemberSummary>` 또는 Page     | Teacher 전용                   |
| PATCH  | `/api/v1/members/{memberId}/deactivate` | body `{ reason? }`                                           | `RsDataVoid`                              | Teacher 전용, 멱등             |
| GET    | `/api/v1/student-profiles`              | `courseId?`, `name?`, `active?`, `pageable`                  | `RsDataPageResponseStudentProfileSummary` | Teacher/Assistant              |
| POST   | `/api/v1/student-profiles`              | `StudentProfileCreateRequest`                                | `RsDataStudentProfileResponse`            | Teacher 전용                   |
| PATCH  | `/api/v1/student-profiles/{id}`         | `StudentProfileUpdateRequest`                                | `RsDataStudentProfileResponse`            | Teacher 전용                   |
| DELETE | `/api/v1/student-profiles/{id}`         | -                                                            | `RsDataVoid`                              | 퇴원/비활성 처리, Teacher 전용 |

## 4. Domain Model (Draft)

- **Member**: active 플래그 기반 비활성화 메서드 추가(이미 있다면 재사용). Repository 쿼리: `findAssistantsByTeacher(teacherId, nameLike, active)`.
- **StudentProfile**: 기존 서비스/리포지토리 확장해 active 필터 지원. 소유권 검증 헬퍼 유지.
- **Auth/Session**: 조교 비활성화 시 Refresh 저장소/세션 무효화 훅 연결.
- **Security**: `@PreAuthorize` 와 도메인 서비스에서 권한 검사 둘다 적용. Teacher 소유/Assistant 소속 검증 유틸 재사용. Assistant는 학생 목록/상세 조회만 허용. Assistant 소속 판단은 초대 가입 시 설정된 `teacherId`(Member.teacherId)로 한다.

## 5. TDD Plan

0. backend/AGENTS.md를 참고하여 TDD 개발 방식을 따른다.
1. **조교 목록**: Service/Controller 테스트 - role=ASSISTANT 필터, 이름 부분 검색, active 필터, 권한(비 Teacher 차단), Teacher별 스코프 검증.
2. **조교 비활성화**: 이미 비활성 시 멱등, 활성→비활성 전환, 다른 Teacher 소유/없는 ID/권한 없는 경우 예외. Refresh 무효화 호출 검증(모킹).
3. **학생 목록 필터**: courseId/name/active 필터 동작, Teacher/Assistant 접근 허용, 타 소유 접근 차단.
4. **학생 CRUD/퇴원**: Teacher만 생성/수정/퇴원 가능, Assistant 수정/퇴원 시 403/404. 퇴원 시 Member.active=false 처리 확인. validation 실패/존재하지 않음 예외 테스트.
5. **문서/스펙**: SpringDoc 경로 노출 및 OpenAPI 스냅샷 검증(가능 시).
