# 📋 개선사항 백로그

베타 릴리즈 이후 순차적으로 해결할 개선/리팩터링 후보를 정리합니다. 각 항목은 목적과 실행 범위를 간결히 남겨두고, 착수 시 별도 이슈·PLAN으로 분리합니다.

---

## 🔐 보안 & 인증

### #1 RefreshToken Rotation 구현
- 우선순위: 🔴 High
- 목적: Refresh Token 재사용(Replay) 공격을 차단하고 탈취 시 즉시 세션 무효화.
- 작업 범위: 토큰 발급/검증 시 DB에 저장된 최신 토큰과 비교, Rotation 및 블랙리스트 처리 로직 추가.

### #2 재발급 시 사용자 상태 검증
- 우선순위: 🔴 High
- 목적: 비활성/탈퇴/정지된 계정이 refresh()를 통해 AccessToken을 재발급받는 문제 차단.
- 작업 범위: RefreshToken 검증 흐름에 `isActive` 등 상태 확인 로직 추가, 실패 시 표준 에러 코드 반환.

### #3 RefreshToken Redis 적용
- 우선순위: 🟡 Medium
- 목적: 서버 확장 시 RDB Lock/부하를 줄이고, 토큰 만료 관리를 간소화.
- 작업 범위: 토큰 저장소를 Redis Hash/Set으로 이전, TTL 관리 및 멀티 노드 동기화 전략 정립.

---

## 🗄️ 인프라 & 배포

### #4 DB 스키마 변경 전략
- 우선순위: 🟡 Medium
- 목적: 현재 create-drop 환경을 버리고 안전한 마이그레이션 파이프라인 확보.
- 작업 범위: Flyway/Liquibase 도입, 로컬/CI/CD 단계에서 버전 관리 및 롤백 전략 수립.

### #5 무중단 배포
- 우선순위: 🟡 Medium
- 목적: 운영 배포 시 세션 끊김 없이 서비스 지속.
- 작업 범위: Blue-Green 또는 Rolling 전략 설계, 헬스체크 및 트래픽 스위칭 자동화.

---

## ✨ 기능 개선

### #6 출강 학원(TeacherBranchAssignment) 수정 기능
- 우선순위: 🟢 Low
- 목적: 잘못 등록된 출강 정보를 선생님이 직접 정정할 수 있도록 지원.
- 작업 범위: PATCH 엔드포인트 추가, 권한 검증 및 감사 로그 작성.

### #7 선생님 StudentEnrollmentRequest 처리 효율화
- 우선순위: 🟡 Medium
- 목적: 대량 신청 처리 시 UX/작업 시간을 줄이기.
- 작업 범위: 일괄 승인·거절, 상태/키워드 필터 추가, 처리 메타데이터 제공.

### #8 학생 목록(StudentManagement) 학생 단위 그룹화
- 우선순위: 🟡 Medium
- 목적: 동일 학생이 여러 반을 듣는 경우 목록에 중복 노출되는 문제 해소, 학생별 진척 파악 용이.
- 작업 범위: `GET /api/v1/student-courses`에 학생 단위 그룹 응답(`StudentCourseGroupResponse`) 추가, StudentCourseManagementService에서 studentId 기반 grouping/페이징, 프런트 `student-management.tsx` UI를 accordion/nested list 형태로 개편. 추후 데이터가 많아지면 학생 기준 쿼리 최적화 필요.

### #9 Progress 공개용 Homework 필드 확장
- 우선순위: 🟢 Low
- 목적: Course/Personal Progress에 숙제(homework) 같은 요약 필드를 추가해 향후 학생이 본인 숙제만 조회할 수 있도록 확장.
- 작업 범위: CourseProgress/PersonalProgress 엔티티 및 DTO에 homework 필드 추가, Teacher만 작성/수정, 학생 전용 읽기 API 설계 및 권한/캘린더 반영. 구현은 Progress CRUD 안정화 후 진행.

### #11 클리닉 슬롯 변경 알림
- 우선순위: 🟡 Medium
- 목적: Slot 요일/시간 변경으로 defaultClinicSlotId가 해제된 학생에게 재신청 안내를 제공.
- 작업 범위: Slot 변경 시 대상 학생 목록 집계, 알림(Notification) 이벤트 발행, 학생 앱에서 재신청 CTA 노출. (Notification 기능 도입 시 구현)

---

## 📱 UI/UX

### #10 모바일(320~420px) UI 최적화
- 우선순위: 🟡 Medium
- 목적: 현재 반응형 UI가 320~420px 휴대폰 환경에서 레이아웃 깨짐/가독성 저하가 발생하는 문제 개선.
- 작업 범위: 주요 페이지 모바일 뷰 점검 및 핵심 플로우 재설계, 필요 시 모바일 전용 레이아웃/컴포넌트 분리, 터치 친화 UI(버튼 크기/간격) 및 스크롤/오버플로우 조정.

---

## 🐛 버그

### #9 어드민 요청 조회 서버 내부 오류
- 우선순위: 🔴 High
- 목적: Admin 요청 조회 시 500 오류 발생 원인을 제거.
- 작업 범위: 최근 배포 로그 분석, 재현 케이스 수집, 원인 패치 및 회귀 테스트.

---
