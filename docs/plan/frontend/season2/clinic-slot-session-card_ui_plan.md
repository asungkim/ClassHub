# Feature: 클리닉 슬롯/세션 카드 정보 강화 UI

## 1. Problem Definition
- 클리닉 슬롯/세션 카드에서 시간대와 수용/참석 현황이 한눈에 보이지 않는다.
- 시간 문자열이 truncation 되어 실제 시간대가 보이지 않는 문제가 있다.

## 2. User Flows & Use Cases
- 선생님/조교가 지점별 슬롯 시간표를 확인할 때:
  - 카드에 시간대(예: 11:50 ~ 14:00)와 기본 슬롯 설정 인원/정원을 함께 확인
- 선생님/조교가 주차별 세션 시간표를 확인할 때:
  - 카드에 시간대와 참석 인원/정원을 함께 확인

## 3. Page & Layout Structure
- 페이지
  - 선생님: /teacher/clinics/slots, /teacher/clinics/sessions
  - 조교: /assistant/clinics/slots, /assistant/clinics/sessions
- WeeklyTimeGrid 카드 내부의 텍스트 라인을 확장해 시간과 카운트를 표시
- `truncate` 제거 및 `whitespace-nowrap`로 시간 문자열 고정 표시

## 4. Component Breakdown
- WeeklyTimeGrid renderItem 사용 영역
  - TeacherClinicSlotsPage: 슬롯 카드
  - AssistantClinicSlotsPage: 슬롯 카드
  - TeacherClinicSessionsPage: 세션 카드
  - AssistantClinicSessionsPage: 세션 카드

## 5. State & Data Flow
- 슬롯
  - API: GET /api/v1/clinic-slots
  - 신규 필드: defaultAssignedCount
  - UI: `{defaultAssignedCount}/{defaultCapacity}` 표시
- 세션
  - API: GET /api/v1/clinic-sessions
  - 사용 필드: attendanceCount, capacity
  - UI: `{attendanceCount}/{capacity}` 표시

## 6. Interaction & UX Details
- 카드 상단에 시간 범위를 고정 표시
- 카드 하단에 인원 수를 표시 (슬롯: 기본 슬롯 설정 인원, 세션: 참석 인원)
- 기존 클릭/편집/삭제 동작은 유지

## 7. Test & Verification Plan
- 수동 QA
  - 슬롯 카드에 시간/인원 정보가 정상 표시되는지 확인
  - 세션 카드에 시간/참석 인원 정보가 정상 표시되는지 확인
  - 긴 시간 문자열이 잘리지 않는지 확인

