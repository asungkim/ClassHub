# Feature: KST 기준 시간 처리 통일 (Backend/DB/Frontend)

## 1. Problem Definition
- 클리닉 슬롯/세션 생성, 출석 배치 등에서 UTC/KST 불일치로 날짜 계산이 틀어지는 문제가 발생한다.
- 현재 서버/DB/프론트가 서로 다른 기준으로 시간을 해석할 수 있어 디버깅이 어렵다.

## 2. Requirements
### Functional
- 모든 서버 내부 시간 계산은 KST 기준으로 수행한다.
- DB 저장 시각(createdAt/updatedAt/deletedAt 등)도 KST 기준으로 저장한다.
- 클리닉 세션 생성/출석 배치가 KST 기준 “오늘/이번 주”로 동작한다.
- 프론트는 KST 기준으로 날짜/시간을 표시한다.

### Non-functional
- 마이그레이션 없이 개발 단계에서 적용한다(기존 데이터 변환 없음).
- 운영 환경에서도 동일한 시간 기준을 유지한다.
- 시간 계산 로직이 테스트 가능하도록 Clock을 주입한다.

## 3. Design
### 3.1 Backend
- JVM 타임존을 KST로 고정한다.
  - 런타임 옵션 또는 컨테이너 환경변수로 `Asia/Seoul` 적용
- `TimeConfig`의 `Clock`을 KST로 변경한다.
  - `Clock.system(ZoneId.of("Asia/Seoul"))`
- JPA Auditing용 DateTimeProvider를 KST 기준으로 추가한다.
  - `LocalDateTime.now(clock)` 반환
- 시간 계산 로직에서 `LocalDate.now()`/`LocalDateTime.now()`를 직접 호출하지 않고, `Clock`을 주입해 사용한다.
  - 대상: `ClinicBatchService`, `ClinicSlotService`, `ClinicDefaultSlotService`, `ClinicAttendanceService`, `ClinicSessionService`
- `BaseTimeEntity.delete()`는 KST Clock 기반으로 설정되도록 공통 유틸 또는 Provider를 사용한다.

### 3.2 DB/Infra
- MySQL 컨테이너 타임존을 KST로 고정한다.
  - `TZ=Asia/Seoul`
  - `--default-time-zone=+09:00`
- (선택) `spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Seoul` 설정

### 3.3 Frontend
- 날짜/시간 표시는 KST 기준으로 통일한다.
- date-only 필드는 `YYYY-MM-DD` 문자열만 전송한다.
- time-only 필드는 `HH:mm` 문자열 그대로 전송한다.
- Week range, session 조회 파라미터 생성 시 KST 기준 날짜 문자열을 사용한다.
  - 필요 시 `formatDateYmdKst` 사용 경로 점검

## 4. Implementation Plan
1. KST Clock 및 JVM 타임존 고정
   - `TimeConfig` KST Clock 적용
   - Docker compose에 `TZ` 및 `default-time-zone` 설정
2. Auditing 및 공통 시간 Provider 적용
   - JPA Auditing DateTimeProvider 추가
   - BaseTimeEntity delete 시간 KST 적용
3. 클리닉 시간 계산 로직 수정
   - 모든 `LocalDate.now()`/`LocalDateTime.now()`를 `Clock` 기반으로 전환
4. 프론트 날짜/시간 생성 경로 점검
   - clinic/week, sessions, attendance 등 날짜 파라미터 생성 로직 KST 기준 확인

## 5. TDD Plan
- `Clock` 기반 시간 계산 테스트 추가
  - `ClinicBatchService.generateRemainingSessionsForSlot`에서 고정 Clock으로 세션 생성 검증
  - `ClinicDefaultSlotService.createAttendancesForCurrentWeek`에서 고정 Clock으로 출석 생성 검증
- `BaseTimeEntity.delete()` 시간 설정 테스트(Clock 고정)
- 날짜 파라미터 생성 유틸/화면 로직(프론트)은 수동 QA로 확인

## 6. Verification
- 금요일 16:00(KST) 기준, 금요일 18:00 슬롯 생성 → 세션 즉시 생성 확인
- 학생이 default slot 설정 시 attendance 생성 확인
- 로그 시간대가 KST로 출력되는지 확인

## 7. Out of Scope
- 기존 데이터 KST 변환 마이그레이션
- 글로벌 타임존 지원
