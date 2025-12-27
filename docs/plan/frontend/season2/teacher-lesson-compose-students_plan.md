# Feature: 통합 수업 작성 모달 - 반 학생 목록 연동

## 1. Problem Definition
- 통합 수업 작성 모달에서 선택한 반의 학생 목록(재원/휴원)과 recordId를 조회할 API가 없어 학생 선택/개인 진도 입력이 불가능하다.

## 2. User Flows & Use Cases
- 모달 오픈 → 반 목록 로딩 → 첫 번째 반 자동 선택 → 학생 목록 로딩
- 반 변경 시 해당 반 학생(재원/휴원) 목록 재조회
- 학생 체크박스 선택 → 개인 진도 입력 카드 생성

## 3. Page & Layout Structure
- 기존 모달 레이아웃 유지
- 학생 목록은 카드형 체크박스 리스트 (휴원 학생은 배지 표시)
- 학생이 없을 경우 빈 상태 메시지 노출

## 4. Component Breakdown
### `TeacherLessonComposeModal`
  - 역할: 반 선택 + 학생 선택 + 개인 진도 입력
  - props/state: `selectedCourseId`, `students`, `selectedStudentIds`, `personalInputs`
  - 이벤트: 반 변경 시 학생 목록 재조회, 체크박스 토글

## 5. State & Data Flow
- `GET /api/v1/courses/{courseId}/students`
  - 응답에서 `recordId`, `student.name`, `assignmentActive` 사용
  - 재원/휴원 모두 반환, 휴원 학생은 UI 배지 표시
  - 반 목록 카드에는 기간 기반 진행 상태(진행 예정/진행 중/종료) 배지 표시
- 로딩/에러 상태는 기존 toast + empty UI 유지

## 6. Interaction & UX Details
- 학생 목록 로딩 중에는 기존 리스트 영역에 로딩 표시
- 학생이 없으면 “선택한 반에 학생이 없습니다” 메시지 유지
- 휴원 학생은 배지로 구분하고(선택 가능 여부는 정책 확인)

## 7. Test & Verification Plan
- `npm run build -- --webpack`로 타입/빌드 확인
- 수동 테스트: 모달 오픈 → 반 변경 → 학생 목록 갱신 → 체크박스 선택 → 개인 진도 입력 노출
