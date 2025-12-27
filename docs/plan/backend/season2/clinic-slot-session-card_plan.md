# Feature: 클리닉 슬롯 카드 카운트 확장

## 1. Problem Definition
- 클리닉 슬롯 카드에 "기본 슬롯 설정 인원 / 정원" 정보를 표시해야 한다.
- 현재 슬롯 목록 응답에는 기본 슬롯 설정 인원 수가 없어 UI에서 계산할 수 없다.

## 2. Requirements

### Functional
- 클리닉 슬롯 목록 응답에 기본 슬롯 설정 인원 수를 포함한다.
  - 기준: StudentCourseRecord.defaultClinicSlotId가 해당 slotId인 레코드 수
  - deletedAt is null 조건만 포함
- 클리닉 슬롯 생성/수정 응답에도 동일 필드를 포함한다.

### Non-functional
- N+1 쿼리를 피하고 슬롯 목록 조회는 단일 집계 쿼리로 처리한다.
- 기존 API 응답 구조는 유지하되 필드만 확장한다.
- RsData/RsCode 규칙 준수.

## 3. API Design (Draft)
- GET /api/v1/clinic-slots
  - ClinicSlotResponse에 `defaultAssignedCount` 필드 추가
- POST /api/v1/clinic-slots
  - 동일 필드 포함
- PATCH /api/v1/clinic-slots/{slotId}
  - 동일 필드 포함

## 4. Domain Model (Draft)
- StudentCourseRecord
  - defaultClinicSlotId 기준 집계용 쿼리 추가
- Repository
  - StudentCourseRecordRepository
    - `countDefaultClinicSlots(List<UUID> slotIds)` (slotId별 count 반환)
- Service
  - ClinicSlotService
    - slotId 리스트 기반 count map 생성 유틸 메서드 추가
- DTO
  - ClinicSlotResponse
    - `defaultAssignedCount` 필드 추가
    - `from(slot, count)` 형태로 확장

## 5. TDD Plan
1. Repository 테스트
   - slotId 리스트 기준 count 집계가 정상적으로 반환되는지 검증
2. Service 테스트
   - 슬롯 목록 + count map 결합 로직 검증
3. Controller 테스트
   - 클리닉 슬롯 목록 응답에 `defaultAssignedCount` 포함 여부 검증

