# Feature: Teacher Course Management API

## 1. Problem Definition
- Teacher가 대시보드 “반 관리” 화면에서 목록/시간표 두 가지 뷰로 Course를 조회하고, 생성/수정/활성화 제어를 수행할 수 있도록 백엔드 API가 필요하다.
- 기존 스펙(v1.3)에는 Course 엔티티와 공개 검색 요건만 정의되어 있어 Teacher 전용 CRUD/스케줄 API가 누락된 상태다.
- 목표: 시작일·종강일 필드를 포함해 Course 상태를 명확히 관리하고, soft delete 대신 활성/비활성 토글(삭제 아님)로 동작하도록 설계/TDD 계획을 수립한다.

## 2. Requirements
### Functional
1. **학원 목록 조회 (재사용)**  
   - `GET /api/v1/teachers/me/branches?status=ACTIVE` (기 구현)  
   - Course 생성 모달과 필터에서 사용할 Branch/Company 정보를 제공한다.
2. **Course 목록 뷰**  
   - `GET /api/v1/courses?branchId&status&keyword&page&size`  
   - 응답에는 `courseId, name, branchName, companyName, schedules[], startDate, endDate, active(Boolean)`가 포함되어야 한다.  
   - status=ACTIVE/INACTIVE/ALL 필터 및 이름 검색 지원.
3. **Course 캘린더 뷰**  
   - `GET /api/v1/courses/schedule?startDate&endDate`  
   - 날짜 범위(예: 월~일) 내에 해당하는 Course와 스케줄을 반환해 1시간 단위 시간표 UI에서 사용.  
   - Branch/Company 이름을 포함해 프런트에서 라벨을 바로 표시할 수 있도록 한다.
4. **Course 생성**  
   - `POST /api/v1/courses`  
   - Body: `{ branchId, name, startDate, endDate, schedules[{ dayOfWeek, startTime, endTime }] }`.  
   - Teacher는 자신의 Branch Assignment가 있는 학원에만 생성 가능, Branch가 UNVERIFIED/비활성인 경우 거부.
5. **Course 상세 조회**  
   - `GET /api/v1/courses/{courseId}`  
   - Course 기본 정보 + Branch/Company + 스케줄 + 활성 상태를 반환.
6. **Course 수정**  
   - `PATCH /api/v1/courses/{courseId}`  
   - Body: `{ name?, startDate?, endDate?, schedules? }` (필요 필드만 부분 수정).  
   - 스케줄 수정 시 기존 ElementCollection을 교체하고 중복/시간 겹침을 검증한다.
7. **Course 활성/비활성 토글**  
   - `PATCH /api/v1/courses/{courseId}/status` with `{ enabled: boolean }`.  
   - enabled=false → 비활성 처리(soft delete 아님), enabled=true → 재활성화.  
   - 비활성 Course는 목록 기본 필터에서 제외되고 공개 검색에도 노출되지 않는다.

### Non-functional
1. Course 엔티티에 `startDate`, `endDate` 컬럼을 추가하고, 기간 검증(startDate ≤ endDate) 로직을 공통으로 적용한다.  
2. 모든 API는 `RsData` 포맷과 `RsCode` 예외 규칙을 따르며, 기존 TeacherBranchAssignment 권한 검증을 재사용한다.  
3. Soft delete 대신 활성/비활성 Boolean을 도출하기 위해 `deletedAt` 필드는 유지하되 “비활성=deletedAt 설정”으로 일관되게 처리하고, “삭제”라는 용어 대신 “비활성화”만 사용한다.  
4. 캘린더 API는 주/월 단위 반복 호출을 고려해 Branch/Company 정보를 join fetch/DTO 조합으로 최소 쿼리 수로 제공한다.  
5. 스케줄 검증 유틸을 별도 클래스로 분리해 Service/Controller 테스트와 독립적으로 단위 테스트를 작성한다.

## 3. API Design (Draft)
| Method | URI | Description | Notes |
| ------ | --- | ----------- | ----- |
| GET    | `/api/v1/teachers/me/branches` | Teacher Branch 목록 (기 구현) | status=ACTIVE |
| GET    | `/api/v1/courses` | 목록 뷰 | branch/keyword/status 필터 + pagination |
| GET    | `/api/v1/courses/schedule` | 캘린더 뷰 | startDate/endDate 범위 |
| POST   | `/api/v1/courses` | Course 생성 | body: branchId, name, start/end, schedules |
| GET    | `/api/v1/courses/{courseId}` | Course 상세 | |
| PATCH  | `/api/v1/courses/{courseId}` | Course 수정 | 이름/기간/스케줄 선택적 변경 |
| PATCH  | `/api/v1/courses/{courseId}/status` | 활성/비활성 전환 | `{ enabled: boolean }` |

**권한/검증**  
- Principal role은 반드시 TEACHER.  
- Service에서 `TeacherBranchAssignmentRepository`로 권한 체크, Branch verified/활성 상태 확인.  
- startDate·endDate·schedule 겹침 검증 실패 시 `RsCode.BAD_REQUEST`.

**Response DTO (예시)**  
```java
record CourseResponse(
    UUID courseId,
    UUID branchId,
    String branchName,
    String companyName,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    boolean active,
    List<CourseScheduleDto> schedules
) {}
```

## 4. Domain Model (Draft)
- Course 엔티티 필드: `branchId`, `teacherMemberId`, `name`, `startDate`, `endDate`, `Set<CourseSchedule> schedules`, `deletedAt`.  
- 메서드:  
  - `create(branchId, teacherId, name, startDate, endDate, schedules)`  
  - `updateInfo(name, startDate, endDate)`  
  - `replaceSchedules(newSchedules)`  
  - `activate() / deactivate()` (`deletedAt` 기반).  
- Repository:  
  - `findByTeacherMemberIdAndDeletedAtIsNull` (Pageable, branch/status/keyword 필터)  
  - `findByIdAndTeacherMemberId`  
  - `searchSchedules(teacherId, startDate, endDate)` for calendar view (custom query with fetch join).

## 5. TDD Plan
1. **Repository Tests**  
   - Soft delete 필터, branch/status/keyword 조건 검증.  
   - Schedule range 쿼리(캘린더용) 검증.  
2. **Service Tests (`CourseServiceTest`)**  
   - create 실패 케이스: 권한 없음, Branch 비활성, 스케줄 겹침, 기간 역전 등.  
   - getCourses 필터 조합, getSchedule 범위 응답.  
   - updateCourse: 이름/기간/스케줄 각각 변경 + validation.  
   - updateStatus: 활성/비활성 토글 및 중복 요청 처리.  
3. **Controller Tests (`CourseControllerTest`)**  
   - 각 API의 인증/인가, request body validation, 성공 응답 구조.  
4. **Validator Tests**  
   - `CourseScheduleValidator`: start < end, 중복 요일·시간 겹침, 비어 있는 배열 처리.  
   - `CoursePeriodValidator`: startDate <= endDate, null 허용 여부 등.

## 6. 구현 계획
1. **Repository 계층**: Course 엔티티에 `startDate/endDate`와 Schedule Embeddable을 추가하고, 목록/캘린더 필터 메서드를 TDD로 완성한다.  
2. **검증 유틸**: `CourseScheduleValidator`, `CoursePeriodValidator`를 별도 클래스로 만들고 단위 테스트로 시간 겹침/기간 역전 케이스를 먼저 보장한다.  
3. **Service 로직**: 권한·Branch 상태 검증을 포함한 `CourseService`를 구현해 생성/수정/스케줄 교체/활성화 토글과 목록/캘린더 응답을 처리한다.  
4. **Controller & DTO**: `/api/v1/courses` 관련 5개 엔드포인트를 Controller에서 선언하고 Request/Response DTO에 `companyName`, 기간, 스케줄 정보를 매핑한다.  
5. **통합 검증**: Controller 테스트로 인증/인가·Validation 에러를 확인한 뒤 `./gradlew test` 및 `npm run build -- --webpack`으로 전체 타입/테스트를 검증한다.
