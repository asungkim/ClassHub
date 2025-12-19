# Feature: Company & Branch Management API

## 1. Problem Definition
- Phase5 첫 작업으로, 선생님이 학원(Company)과 지점(Branch)을 등록/조회하고, SuperAdmin이 검증(verifiedStatus) 및 비활성화(soft delete)를 제어할 수 있는 백엔드 API가 필요하다.
- 기존 Season1 invite 기반 흐름과 달리, Season2에서는 Company/Branch 구조가 Course/Enrollment의 기반이 되므로, Role별 CRUD 및 검증 로직을 명확히 정리해야 한다.

## 2. Requirements
### Functional
**Teacher**
1. `POST /companies`  
   - INDIVIDUAL: 이름/설명만 받아 Company + Branch를 동시에 생성, verifiedStatus=VERIFIED.  
   - ACADEMY: verifiedStatus=UNVERIFIED로 등록, 선택적으로 Branch 생성/연결.  
2. `GET /companies?verifiedStatus&type&keyword`  
   - 기본적으로 VERIFIED만 조회; 본인이 생성한 UNVERIFIED도 포함.  
3. `POST /branches`  
   - 특정 Company 하위에 Branch 생성. INDIVIDUAL은 자동 생성 1개 제한, ACADEMY는 다수 허용.  
4. `GET /branches?companyId`  
   - 본인이 접근 가능한 Company의 Branch 목록 조회(UNVERIFIED는 생성자만).  
5. `PATCH /branches/{id}`  
   - OWNER(INDIVIDUAL) 또는 SuperAdmin 권한만 이름 수정/soft delete 가능.

**SuperAdmin**
1. `GET /companies?verifiedStatus=UNVERIFIED` / `GET /branches?verifiedStatus=UNVERIFIED`  
   - 검증 대기 목록 조회.  
2. `PATCH /companies/{id}/verified-status` / `PATCH /branches/{id}/verified-status`  
   - VERIFIED/UNVERIFIED 전환 및 soft delete (deletedAt 설정).  
3. `GET /companies/{id}` / `GET /branches/{id}`  
   - 감사/검증 용 상세 조회(Teacher도 사용 가능).

**Assistant/Student**  
- Company/Branch를 직접 조작하지 않음. 공개 Course 검색 시 VERIFIED 상태의 Company/Branch만 참조.

### Non-functional
- 모든 엔드포인트는 `RsData` 응답을 사용하고, Role 기반 인가를 `@PreAuthorize`로 강제한다.
- Soft delete (`deletedAt`) 및 `verifiedStatus`에 따라 기본 조회 조건을 분기한다.
- Teacher 생성 시 audit 정보를 위해 `creatorMemberId`를 기록한다.
- 요청 DTO에 validation(`@NotBlank`, enum 검증 등)을 적용하고, Service 계층에서 비즈니스 규칙을 검사한다.

## 3. API Design (Draft)

| Method | URL | Request | Response | Role |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/companies` | `{ name, description?, type }` | `RsData<CompanyResponse>` | TEACHER |
| GET | `/api/v1/companies` | `verifiedStatus?, type?, keyword?, page?, size?` | `RsData<Page<CompanyResponse>>` | TEACHER/SUPER_ADMIN |
| PATCH | `/api/v1/companies/{id}/verified-status` | `{ verified: boolean }` | `RsData<CompanyResponse>` | SUPER_ADMIN |
| POST | `/api/v1/branches` | `{ companyId, name }` | `RsData<BranchResponse>` | TEACHER |
| GET | `/api/v1/branches` | `companyId?, verifiedStatus?, keyword?` | `RsData<List<BranchResponse>>` | TEACHER/SUPER_ADMIN |
| PATCH | `/api/v1/branches/{id}` | `{ name? }` or `{ enabled: boolean }` | `RsData<BranchResponse>` | TEACHER(OWNER)/SUPER_ADMIN |
| PATCH | `/api/v1/branches/{id}/verified-status` | `{ verified: boolean }` | `RsData<BranchResponse>` | SUPER_ADMIN |

`CompanyResponse` 및 `BranchResponse`는 id, name, verifiedStatus, creator info, createdAt 등을 포함.

## 4. Domain Model (Draft)
- **Company**  
  - 필드: `name`, `description`, `type (INDIVIDUAL/ACADEMY)`, `verifiedStatus`, `creatorMemberId`, `deletedAt`.  
  - 비즈니스 규칙: INDIVIDUAL은 Branch 한 개 + 자동 VERIFIED, 생성 교사만 접근. ACADEMY는 UNVERIFIED 상태로 생성 후 SuperAdmin 검증.
- **Branch**  
  - 필드: `companyId`, `name`, `creatorMemberId`, `verifiedStatus`, `deletedAt`.  
  - INDIVIDUAL Branch는 Company 생성 시 자동 생성되며 수정/삭제 권한은 해당 Teacher(OWNER) 또는 SuperAdmin.  
  - ACADEMY Branch는 FREELANCE Teacher가 생성 가능하나 verifiedStatus=UNVERIFIED로 시작.
- **DTOs / Services**  
  - `CompanyCommandService`: 생성/검증/soft delete 로직, Teacher 권한 검증.  
  - `BranchCommandService`: Branch 생성/수정/검증.  
  - `CompanyQueryService`, `BranchQueryService`: verifiedStatus/creator 조건을 반영한 조회.

## 5. TDD Plan
1. **Repository Tests**  
   - CompanyRepository: `findByVerifiedStatusAndKeyword` + soft delete 제외 조건 검증.  
   - BranchRepository: companyId/verifiedStatus 필터, creatorMemberId 접근 제한 테스트.
2. **Service Tests**  
   - CompanyCommandService:  
     - Teacher 생성 시 INDIVIDUAL 자동 Branch 생성, ACADEMY UNVERIFIED 설정.  
     - SuperAdmin 검증/soft delete 토글.  
     - 권한 없는 교사의 Branch 수정 시 예외.  
   - BranchCommandService: Branch 생성/수정, OWNER 검증, verifiedStatus 전환.
3. **Controller Tests**  
   - Role별 인가 (`@PreAuthorize`).  
   - Query 파라미터 처리(verifiedStatus 기본값).  
   - 검증 실패 시 `RsCode.BAD_REQUEST`.

## 6. 3단계 개발 순서
1. **Repository & Entity 확장**  
   - Company/Branch 엔티티 verifiedStatus/creator 필드 최종 점검, Repository 메서드 작성 및 DataJpaTest로 필터 동작 검증.
2. **Service & DTO 구현**  
   - CompanyCommandService/BranchCommandService + QueryService를 작성하고, Role 검증/soft delete/verifiedStatus 전환 로직을 Mockito 단위 테스트로 보장.
3. **Controller & 통합 검증**  
   - `/api/v1/companies`, `/api/v1/branches` 및 SuperAdmin 전용 검증 API 컨트롤러를 추가하고 MockMvc 테스트로 인가/요청 파라미터/응답 구조를 검증.

---

### 계획 요약 (한국어)
- Teacher는 학원을 등록하고 Branch를 추가/조회하며, SuperAdmin은 검증/비활성화를 담당한다는 역할 분리를 기반으로 Company/Branch CRUD API를 설계했다.  
- 각 Role별 접근 권한과 verifiedStatus/soft delete 규칙을 Service/Repository 레벨에서 검증하고, Controller는 RsData 응답과 필터 파라미터를 제공하도록 설계했다.  
- Repository → Service → Controller 순으로 TDD를 진행해 INDIVIDUAL vs ACADEMY 동작 차이와 SuperAdmin 검증 플로우를 검증한다.
