# Feature: 클리닉 슬롯/세션 카드 정보 강화 UI

## 1. Problem Definition
- 클리닉 슬롯/세션 카드에서 시간대와 수용/참석 현황이 한눈에 보이지 않는다.
- 시간 문자열이 truncation 되어 실제 시간대가 보이지 않는 문제가 있다.

## 2. User Flows & Use Cases
- 선생님/조교가 지점별 슬롯 시간표를 확인할 때:
  - 카드에 시간대(예: 11:50 ~ 14:00)와 기본 슬롯 설정 인원/정원을 함께 확인
- 선생님/조교가 주차별 세션 시간표를 확인할 때:
  - 카드에 시간대와 참석 인원/정원을 함께 확인
- 학생이 기본 클리닉 슬롯을 선택할 때:
  - 슬롯 카드에 현재 선택 인원/정원을 확인하고 만석 여부를 즉시 확인
  - 슬롯 변경 후 즉시 목록을 새로고침해 내가 들어간 숫자를 반영
- 학생이 이번 주 클리닉 세션을 확인할 때:
  - 세션 카드에 참석 인원/정원을 표시하고 참석 세션은 파란색 유지
  - 참석 신청/이동 후 즉시 새로고침해 숫자를 반영

## 3. Page & Layout Structure
- 페이지
  - 선생님: /teacher/clinics/slots, /teacher/clinics/sessions
  - 조교: /assistant/clinics/slots, /assistant/clinics/sessions
  - 학생: /student/clinics/schedule, /student/clinics/week
- WeeklyTimeGrid 카드 내부의 텍스트 라인을 확장해 시간과 카운트를 표시
- `truncate` 제거 및 `whitespace-nowrap`로 시간 문자열 고정 표시

## 4. Component Breakdown
- WeeklyTimeGrid renderItem 사용 영역
  - TeacherClinicSlotsPage: 슬롯 카드
  - AssistantClinicSlotsPage: 슬롯 카드
  - TeacherClinicSessionsPage: 세션 카드
  - AssistantClinicSessionsPage: 세션 카드
  - StudentClinicSchedulePage: 슬롯 카드
  - StudentClinicWeekPage: 세션 카드

## 5. State & Data Flow
- 슬롯
  - API: GET /api/v1/clinic-slots
  - 신규 필드: defaultAssignedCount
  - UI: `{defaultAssignedCount}/{defaultCapacity}` 표시
  - 학생 기본 슬롯 설정 후 즉시 재조회하여 count 갱신
- 세션
  - API: GET /api/v1/clinic-sessions
  - 사용 필드: attendanceCount, capacity
  - UI: `{attendanceCount}/{capacity}` 표시
  - 학생 참석 신청/이동 후 세션 재조회하여 count 갱신

## 6. Interaction & UX Details
- 카드 상단에 시간 범위를 고정 표시
- 카드 하단에 인원 수를 표시 (슬롯: 기본 슬롯 설정 인원, 세션: 참석 인원)
- 기존 클릭/편집/삭제 동작은 유지
- 학생 화면에서는 만석(`count >= capacity`) 상태를 배지/색상으로 표시
  - 참석 중인 세션은 파란색 유지, 만석 배지를 함께 표시
  - 비참석 세션이 만석이면 비활성 톤으로 표시

## 7. Test & Verification Plan
- 수동 QA
  - 슬롯 카드에 시간/인원 정보가 정상 표시되는지 확인
  - 세션 카드에 시간/참석 인원 정보가 정상 표시되는지 확인
  - 긴 시간 문자열이 잘리지 않는지 확인
  - 학생 기본 슬롯 변경 후 count가 갱신되는지 확인
  - 학생 세션 참석 신청/이동 후 count가 갱신되는지 확인

## 8. Implementation Steps
### 1단계: 선생님/조교
- 대상 페이지
  - /teacher/clinics/slots
  - /assistant/clinics/slots
  - /teacher/clinics/sessions
  - /assistant/clinics/sessions
- 변경 사항
  - 슬롯 카드: `defaultAssignedCount / defaultCapacity` 표시
  - 세션 카드: `attendanceCount / capacity` 표시
  - 시간 문자열 `truncate` 제거, `whitespace-nowrap` 적용

### 2단계: 학생
- 대상 페이지
  - /student/clinics/schedule
  - /student/clinics/week
- 변경 사항
  - 슬롯 카드: `defaultAssignedCount / defaultCapacity` 표시 + 만석 표시
  - 세션 카드: `attendanceCount / capacity` 표시 + 만석 표시
  - 슬롯 변경/세션 참석·이동 후 즉시 재조회로 카운트 반영
