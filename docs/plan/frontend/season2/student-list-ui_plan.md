# Feature: 학생 목록/상세 모달 UI 개선

## 1. Problem Definition
- 학생 목록이 Course 기준이라 동일 학생이 여러 줄로 노출된다.
- 비활성 Course가 목록에 남아 혼동된다.
- 학생 상세는 Record 단일 기준이라 여러 Course/Record를 한 화면에서 보기 어렵다.

## 2. User Flows & Use Cases
1) Teacher/Assistant 학생 목록
   - 기본 탭 진입 → 학생 단위 목록 조회
   - 필터: Course(선택), 상태(활성/비활성/전체), 이름 검색
   - 목록은 학생 단위로 1명씩 표시
2) Teacher 상세 모달
   - 학생 클릭 → 학생 정보 + Course 목록 + Record 목록 표시
   - 기본은 전체 Course 표시
   - Course를 선택하면 해당 Course의 Record가 표시/수정 가능

## 3. Page & Layout Structure
- 경로: `/teacher/students`, `/assistant/students`
- 구조
  - 상단: 페이지 헤더
  - 탭: 학생 목록 / 신청 처리
  - 학생 목록 탭
    - 필터 카드
    - 목록 테이블
    - 상세 모달
- 반응형
  - 모바일에서는 테이블을 가로 스크롤 유지
  - 상세 모달 내부는 세로 스택으로 전환

## 4. Component Breakdown
- StudentManagementView
  - 역할(Teacher/Assistant)에 맞는 헤더 문구
  - 탭 스위칭 유지
- StudentsTab
  - 학생 목록 필터 + 목록 + 상세 모달 트리거
  - 기존 `StudentCourseListItemResponse` 대신 새 응답 타입 사용
  - 목록 컬럼은 학생 정보 중심(학생명/연락처/학교/학년/나이/상태)
- StudentDetailModal
  - 학생 정보 섹션(최상단)
  - Course 리스트 섹션 (전체/활성/비활성 선택 필터 가능)
  - Record 상세 섹션
    - 선택된 Course에 매핑된 Record 표시
    - 조교/클리닉 슬롯은 드롭다운으로 선택
    - 기존 수정 기능 그대로 사용 (recordId 기반 PATCH)

## 5. State & Data Flow
- 학생 목록 API
  - `GET /api/v1/student-courses/students`
  - 응답 타입: `StudentStudentListItemResponse`
  - 목록 데이터는 `student` + `active`를 사용
- 학생 상세 API
  - `GET /api/v1/student-courses/students/{studentId}`
  - 응답 타입: `StudentStudentDetailResponse`
  - `courses[]`, `records[]`를 받아서 Course 선택 → Record 표시
- Record 수정
  - 기존 `PATCH /api/v1/student-courses/{recordId}` 그대로 사용
  - 수정 후 상세 응답 상태를 갱신
- 조교/클리닉 슬롯 옵션
  - `GET /api/v1/teachers/me/assistants`로 조교 목록 로드
  - `GET /api/v1/clinic-slots?branchId=`로 선택된 반 지점 슬롯 목록 로드

## 6. Interaction & UX Details
- 목록
  - 학생 단위로 1행
  - 상태 배지는 `active` 기준
  - 학생 ID는 화면에서 숨기되, 상세 모달 호출에 사용
- 상세 모달
  - 상단: 학생 정보 카드
  - 중간: Course 리스트(활성/비활성 표시, 선택 상태 강조)
  - 하단: 선택된 Course의 Record 편집 영역
  - Course 선택 시 Record가 없으면 “기록 없음” 안내
- 접근성
  - 탭 이동/모달 포커스 유지

## 7. Test & Verification Plan
- 단위 테스트: 없음(현 단계)
- 통합 테스트
  - `npm run build -- --webpack` 통과
- 수동 QA
  - Teacher: 목록 중복 제거 확인
  - 상태 필터(활성/비활성/전체) 동작 확인
  - 상세 모달에서 Course 선택 → Record 표시/수정 흐름 확인
  - Assistant: 목록만 확인 가능, 상세 접근 차단 유지
