# Feature: Season2 Backend Roadmap

## 1. Problem Definition
- 현재 도메인 코드는 Auth/Member만 남겨두고 나머지를 `domain/notuse`로 치운 상태라, `docs/design/final-entity-spec.md`와 `docs/design/full-erd.md`의 VerifiedStatus/Assignment 기반 구조를 새로 구현해야 한다.
- Company/Branch/Assignment → Enrollment → Lesson/Clinic → Invitation/Notice/WorkLog 순으로 계층적으로 의존하므로, 잘못된 순서로 만들면 다시 뜯어야 하는 상황이 반복된다.
- Season2 기간 동안 어떤 순서로 도메인과 API를 복원할지 명확한 계획이 없어서, TODO Phase/PLAN 기준으로 작업 큐를 정리할 필요가 있다.

## 2. Requirements
### Functional
1. **조직 기반**: Company/Branch VerifiedStatus 및 TeacherBranchAssignment(OWNER/FREELANCE) 재구현.
2. **멤버/학생 정보**: Member + StudentInfo/TeacherInfo, Auth 흐름(회원가입/초대) 복원.
3. **Course/Enrollment/Record**: StudentEnrollmentRequest → StudentCourseEnrollment/Record → TeacherNotes/defaultClinicSlot/담당 조교.
4. **Lesson/Clinic**: SharedLesson/PersonalLesson/ClinicSlot/ClinicSession/ClinicAttendance/ClinicRecord.
5. **Collaboration**: Invitation, Notice/NoticeRead, WorkLog, Feedback, StudentCalendar aggregation.

### Non-functional
- VerifiedStatus 플래그와 auditing을 모든 Company/Branch 변경에 적용.
- SuperAdmin은 모든 API 접근 가능(스펙 §4 주석) → 공통 Security/Aspect에서 권한 예외 처리.
- 기존 `.bak` 소스는 Reference로만 남기고 Season2 작업물은 `domain/**` 새 패키지에서 TDD+리팩터링 진행.

## 3. API Design (Draft)
- **Company/Branch**: `/companies`, `/branches` (POST/GET/PATCH) + SuperAdmin `PATCH /verified-status`.
- **Teacher Relationships**: Assignment는 API 없이 Company/Branch/Course 로직에서 자동 생성/토글.
- **Enrollment**: `/student-enrollment-requests`, `/student-courses`, `/students/me/courses`.
- **Lessons/Clinics**: `/shared-lessons`, `/personal-lessons`, `/clinic-slots`, `/clinic-sessions`, `/clinic-attendances`, `/clinic-records`.
- **Collaboration/Utility**: `/invitations`, `/notices`, `/notices/{id}/reads`, `/work-logs`, `/feedback`, `/students/{id}/calendar`.

## 4. Domain Model (Draft)
- `VerifiedStatus` enum을 Company/Branch 공용 → INDIVIDUAL=VERIFIED 자동, ACADEMY=UNVERIFIED 기본.
- Branch.creatorMemberId로 생성자 소유권 기록, TeacherBranchAssignment.role=OWNER/FREELANCE.
- StudentCourseRecord에 `assistantMemberId`, `defaultClinicSlotId`, `isActive` 유지, ClinicAttendance는 Record FK 기준.
- Invitation → TeacherAssistantAssignment 자동 생성, StudentCalendar Aggregation은 Lesson/Clinic/Record 통합.

## 5. TDD Plan
1. **Step 0. 공통 준비**: VerifiedStatus enum, 공통 Repository/Config, 기존 Auth/Member 모듈 점검.
2. **Step 1. Company/Branch/Assignment**
   - VerifiedStatus + creatorMemberId 필드와 Repository 구현.
   - Branch 생성/조회/검증, Assignment 자동 생성/토글 테스트.
3. **Step 2. Member/StudentInfo/TeacherInfo**
   - Auth + Profile API 보강, StudentInfo CRUD, SuperAdmin 권한 확인.
4. **Step 3. Course & Enrollment Pipeline**
   - StudentEnrollmentRequest, StudentCourseEnrollment/Record, defaultClinicSlot/assistantMemberId 갱신 API.
5. **Step 4. Lesson & Clinic**
   - SharedLesson/PersonalLesson → StudentCalendar 입력 소스 구현.
   - ClinicSlot/Session/Attendance/Record + 학생 주차 이동 로직 테스트.
6. **Step 5. Collaboration/Feedback**
   - Invitation 링크 + 조교 가입, Notice/NoticeRead, WorkLog, Feedback, StudentCalendar Query 서비스 캐싱 없이 우선 구현.
7. **Step 6. Cleanup & Migration**
   - `domain/notuse` 코드 제거, 데이터 마이그레이션 스크립트, 문서/스펙 업데이트 최종 검증.
