# Feature: ClinicSlot 엔티티 및 CRUD

## 1. Problem Definition

- 선생님이 클리닉(보충 수업) 시간표를 요일·시간 단위로 정의하고 관리할 수 있는 ClinicSlot 엔티티가 필요하다.
- ClinicSlot은 반복되는 클리닉 시간표의 "템플릿"으로, 특정 요일과 시간대, 정원을 가지며, 이를 기반으로 매주 실제 ClinicSession이 생성된다.
- Teacher만 ClinicSlot을 생성/수정/비활성화할 수 있으며, 비활성화된 슬롯은 더 이상 세션 생성에 사용되지 않는다.
- ClinicSlot 생성 시 학생들이 선택 가능한 시간대를 제공하고, 학생별로 기본 클리닉 시간을 배정할 수 있도록 한다.

## 2. Requirements

### Functional

- **ClinicSlot 엔티티 속성**

  - `teacherId`: 슬롯을 생성한 선생님 (FK → Member.id, 필수)
  - `dayOfWeek`: 요일 (ENUM: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, 필수)
  - `startTime`: 시작 시간 (LocalTime, 필수, 예: "14:00")
  - `endTime`: 종료 시간 (LocalTime, 필수, 예: "16:00", startTime보다 늦어야 함)
  - `capacity`: 정원 (int, 필수, 기본값 10, 최소 1, 최대 50)
  - `isActive`: 활성 상태 (boolean, 기본값 true)
  - BaseEntity 상속 (id, createdAt, updatedAt)

- **CRUD 기능**

  1. **생성**: Teacher가 새로운 ClinicSlot 생성 (요일, 시간, 정원 지정)
  2. **목록 조회**: Teacher가 본인이 생성한 ClinicSlot 목록 조회 (활성/비활성 필터 가능, 요일별 필터 가능)
  3. **상세 조회**: 특정 ClinicSlot의 상세 정보 조회
  4. **수정**: ClinicSlot 정보(요일, 시간, 정원) 수정
  5. **삭제**: ClinicSlot 물리 삭제 (향후 ClinicSession 연관 시 제약 검토)
  6. **비활성화**: `isActive`를 false로 변경 (향후 세션 생성 중단)
  7. **활성화**: 비활성화된 ClinicSlot을 다시 `isActive`를 true로 변경

- **비즈니스 규칙**

  - 동일 Teacher의 같은 요일/시간대에 중복 슬롯 생성 불가 (활성 슬롯 기준)
    - 예: 월요일 14:00-16:00 슬롯이 이미 있으면, 월요일 15:00-17:00은 생성 불가 (시간 겹침)
  - startTime < endTime 검증 필수
  - 슬롯 수정 시에도 기존 활성 슬롯과의 시간 충돌 검증 필요
  - 비활성화된 슬롯은 목록 조회 시 기본적으로 제외 (필터로 포함 가능)

- **권한**

  - Teacher만 본인이 생성한 ClinicSlot에 대해 CRUD 수행 가능
  - Controller: `@PreAuthorize("hasAuthority('TEACHER')")` + `principal null` 체크
  - Service: `validateTeacher(UUID teacherId)` - Member 존재 및 TEACHER 역할 검증
  - Service: `getClinicSlotOwnedByTeacher(UUID slotId, UUID teacherId)` - 소유권 검증
  - Assistant, Student는 ClinicSlot 조회만 가능 (추후 확장: 학생이 본인 선택 가능한 슬롯 조회)

- **추가 고려사항**
  - **정원 초과 방지**: ClinicSession 생성 시 capacity를 넘는 학생이 배정되지 않도록 체크 (향후 ClinicSession 구현 시 연계)

### Non-functional

- ClinicSlot 엔티티는 `global.entity.BaseEntity` 상속
- Lombok `@Builder`, `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor(access = PROTECTED)` 적용
- 테이블명: `clinic_slot` (snake_case)
- teacherId는 UUID로만 저장 (JPA 관계 매핑 없이 단순 컬럼)
- 서비스 계층 `@Transactional` 적용
- 입력 검증: `jakarta.validation` 애노테이션 사용
- dayOfWeek는 Java 기본 `DayOfWeek` ENUM 활용 (MONDAY ~ SUNDAY)
- 시간 충돌 검증은 서비스 레이어에서 수행

## 3. API Design (Draft)

### 3.1 생성

**POST** `/api/v1/clinic-slots`

**Request Body**

```json
{
  "dayOfWeek": "MONDAY",
  "startTime": "14:00",
  "endTime": "16:00",
  "capacity": 10
}
```

**Response** (성공 시 201)

```json
{
  "code": 201,
  "message": "생성 성공",
  "data": {
    "id": "uuid-string",
    "teacherId": "teacher-uuid",
    "dayOfWeek": "MONDAY",
    "startTime": "14:00",
    "endTime": "16:00",
    "capacity": 10,
    "isActive": true,
    "createdAt": "2025-12-15T10:00:00",
    "updatedAt": "2025-12-15T10:00:00"
  }
}
```

**에러 케이스**

- 400: 시간 검증 실패 (startTime >= endTime) 또는 capacity 범위 초과
- 409: 같은 요일/시간대에 이미 활성 슬롯 존재 (시간 충돌)

### 3.2 목록 조회

**GET** `/api/v1/clinic-slots?isActive=true&dayOfWeek=MONDAY`

**Query Parameters**

- `isActive` (optional, boolean): 활성 여부 필터 (기본값: true)
- `dayOfWeek` (optional, enum): 요일 필터

**Response** (성공 시 200)

```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": [
    {
      "id": "uuid-string",
      "teacherId": "teacher-uuid",
      "dayOfWeek": "MONDAY",
      "startTime": "14:00",
      "endTime": "16:00",
      "capacity": 10,
      "isActive": true,
      "createdAt": "2025-12-15T10:00:00",
      "updatedAt": "2025-12-15T10:00:00"
    }
  ]
}
```

### 3.3 상세 조회

**GET** `/api/v1/clinic-slots/{slotId}`

**Response** (성공 시 200)

```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": {
    "id": "uuid-string",
    "teacherId": "teacher-uuid",
    "dayOfWeek": "MONDAY",
    "startTime": "14:00",
    "endTime": "16:00",
    "capacity": 10,
    "isActive": true,
    "createdAt": "2025-12-15T10:00:00",
    "updatedAt": "2025-12-15T10:00:00"
  }
}
```

**에러 케이스**

- 404: ClinicSlot not found
- 403: 소유권 없음 (다른 Teacher의 슬롯)

### 3.4 수정

**PATCH** `/api/v1/clinic-slots/{slotId}`

**Request Body** (수정할 필드만 포함)

```json
{
  "dayOfWeek": "TUESDAY",
  "startTime": "15:00",
  "endTime": "17:00",
  "capacity": 15
}
```

**Response** (성공 시 200)

```json
{
  "code": 200,
  "message": "수정 성공",
  "data": {
    "id": "uuid-string",
    "teacherId": "teacher-uuid",
    "dayOfWeek": "TUESDAY",
    "startTime": "15:00",
    "endTime": "17:00",
    "capacity": 15,
    "isActive": true,
    "createdAt": "2025-12-15T10:00:00",
    "updatedAt": "2025-12-15T10:30:00"
  }
}
```

**에러 케이스**

- 400: 시간 검증 실패
- 409: 수정 후 다른 슬롯과 시간 충돌
- 404: ClinicSlot not found
- 403: 소유권 없음

### 3.5 삭제

**DELETE** `/api/v1/clinic-slots/{slotId}`

**Response** (성공 시 200)

```json
{
  "code": 200,
  "message": "삭제 성공",
  "data": null
}
```

**에러 케이스**

- 404: ClinicSlot not found
- 403: 소유권 없음

### 3.6 비활성화

**PATCH** `/api/v1/clinic-slots/{slotId}/deactivate`

**Response** (성공 시 200)

```json
{
  "code": 200,
  "message": "비활성화 성공",
  "data": null
}
```

**에러 케이스**

- 404: ClinicSlot not found
- 403: 소유권 없음

### 3.7 활성화

**PATCH** `/api/v1/clinic-slots/{slotId}/activate`

**Response** (성공 시 200)

```json
{
  "code": 200,
  "message": "활성화 성공",
  "data": null
}
```

**에러 케이스**

- 409: 활성화 시 다른 활성 슬롯과 시간 충돌
- 404: ClinicSlot not found
- 403: 소유권 없음

## 5. TDD Plan

### Phase 1: 엔티티 및 리포지토리 테스트

**ClinicSlotTest.java**

1. ✅ `shouldCreateClinicSlot_withValidData()` - 엔티티 생성 검증
2. ✅ `shouldUpdateSlot_whenValidData()` - 슬롯 수정 메서드 검증
3. ✅ `shouldDeactivateSlot()` - 비활성화 메서드 검증
4. ✅ `shouldActivateSlot()` - 활성화 메서드 검증

**ClinicSlotRepositoryTest.java**

1. ✅ `shouldSaveClinicSlot()` - 슬롯 저장
2. ✅ `shouldFindByTeacherIdAndIsActive()` - Teacher별 활성 슬롯 조회
3. ✅ `shouldFindByTeacherIdAndDayOfWeekAndIsActive()` - 요일 필터 조회
4. ✅ `shouldFindByIdAndTeacherId()` - 소유권 검증용 조회
5. ✅ `shouldFindOverlappingSlots_whenTimeConflicts()` - 시간 충돌 쿼리 검증
6. ✅ `shouldNotFindOverlappingSlots_whenNoConflict()` - 충돌 없는 경우

### Phase 2: 서비스 레이어 테스트

**ClinicSlotServiceTest.java**

1. ✅ `shouldCreateSlot_whenValidRequest()` - 슬롯 생성 성공
2. ✅ `shouldThrowException_whenInvalidTeacher()` - 잘못된 Teacher ID
3. ✅ `shouldThrowException_whenStartTimeAfterEndTime()` - 시간 검증 실패
4. ✅ `shouldThrowException_whenTimeConflict()` - 시간 충돌 시 생성 실패
5. ✅ `shouldGetSlots_whenActiveFilter()` - 활성 슬롯 목록 조회
6. ✅ `shouldGetSlots_whenDayOfWeekFilter()` - 요일 필터 조회
7. ✅ `shouldGetSlot_whenValidOwner()` - 슬롯 상세 조회 성공
8. ✅ `shouldThrowException_whenNotOwner()` - 소유권 없는 경우 조회 실패
9. ✅ `shouldUpdateSlot_whenValidData()` - 슬롯 수정 성공
10. ✅ `shouldThrowException_whenUpdateCausesConflict()` - 수정 시 시간 충돌
11. ✅ `shouldDeleteSlot_whenValidOwner()` - 삭제 성공
12. ✅ `shouldThrowException_whenDeleteNotOwner()` - 삭제 권한 없음
13. ✅ `shouldDeactivateSlot_whenValidOwner()` - 비활성화 성공
14. ✅ `shouldActivateSlot_whenNoConflict()` - 활성화 성공
15. ✅ `shouldThrowException_whenActivateCausesConflict()` - 활성화 시 충돌

### Phase 3: 컨트롤러 테스트

**ClinicSlotControllerTest.java** (`@WebMvcTest`)

1. ✅ `shouldCreateSlot_whenTeacherAuthenticated()` - POST 슬롯 생성 (201)
2. ✅ `shouldReturn400_whenInvalidRequest()` - 유효성 검증 실패 (400)
3. ✅ `shouldReturn403_whenNotTeacher()` - 권한 없음 (403)
4. ✅ `shouldGetSlots_withFilters()` - GET 목록 조회 (200)
5. ✅ `shouldGetSlot_whenValidId()` - GET 상세 조회 (200)
6. ✅ `shouldReturn404_whenSlotNotFound()` - 슬롯 없음 (404)
7. ✅ `shouldUpdateSlot_whenValidData()` - PATCH 수정 (200)
8. ✅ `shouldDeleteSlot_whenValidOwner()` - DELETE 삭제 (200)
9. ✅ `shouldReturn404_whenDeleteNotFound()` - 삭제 대상 없음 (404)
10. ✅ `shouldDeactivateSlot()` - PATCH 비활성화 (200)
11. ✅ `shouldActivateSlot()` - PATCH 활성화 (200)

### Phase 4: 통합 테스트

**ClinicSlotIntegrationTest.java** (`@SpringBootTest`)

1. ✅ `shouldCreateAndRetrieveSlot_endToEnd()` - 생성 → 조회 전체 흐름
2. ✅ `shouldPreventTimeConflict_endToEnd()` - 충돌 방지 시나리오
3. ✅ `shouldUpdateAndDelete_endToEnd()` - 수정 → 삭제 흐름
4. ✅ `shouldDeactivateAndActivate_endToEnd()` - 비활성화 → 활성화 흐름
5. ✅ `shouldFilterByDayOfWeek_endToEnd()` - 요일별 조회 시나리오

### Phase 5: RsCode 확인

**기존 RsCode 활용**

- `RsCode.SUCCESS` (200) - 성공
- `RsCode.CREATED` (201) - 생성 성공
- `RsCode.BAD_REQUEST` (400) - 잘못된 요청 (시간 검증 실패 등)
- `RsCode.FORBIDDEN` (403) - 권한 없음
- `RsCode.NOT_FOUND` (404) - 리소스 없음
- `RsCode.CONFLICT` (409) - 시간 충돌

### 6.4 보안

- Teacher만 본인 슬롯 접근 가능 (서비스 레이어에서 소유권 검증)
- JWT 기반 인증 필수

---

## 7. 구현 순서

1. **ClinicSlot 엔티티 작성** - 도메인 모델 및 엔티티 테스트 (TDD)
2. **ClinicSlotRepository 작성** - 리포지토리 인터페이스 및 테스트 (TDD)
3. **DTO 작성** - Request/Response 레코드
4. **ClinicSlotService 작성** - 비즈니스 로직 및 테스트 (TDD)
5. **ClinicSlotController 작성** - REST API 및 테스트 (TDD)
6. **통합 테스트** - End-to-End 시나리오
7. **Swagger 문서 확인** - API 명세 검증

---

이 계획이 승인되면 TDD 방식으로 구현을 시작합니다.
