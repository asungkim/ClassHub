# Feature: 개발 표준 (Java + TypeScript)

## 1. Problem Definition

- 백엔드(Java/Spring)와 프런트엔드(TypeScript/React/Next.js) 전반에 명확한 코드 컨벤션이 없어 스타일이 불일치하고 리뷰 효율이 떨어진다. 채택/강제가 쉬운 실용적 표준을 수립해야 한다.

## 2. Requirements

### Functional

- Java(Spring Boot)와 TypeScript(React/Next.js) 스타일 가이드를 문서로 제공한다.
- 레이어별 네이밍, 파일/폴더 구조, 코드 구성 패턴을 정의한다(백엔드: controller/service/repository/domain, 프런트엔드: app/features/components/hooks/libs).
- 테스트 규칙(테스트 네이밍, 구조, MockMvc/통합 테스트 활용 시점, 컴포넌트/통합 테스트)을 명확히 한다.
- 표준을 강제할 도구(포매터/린터)를 제안하되, 실제 설정 추가는 승인 이후 별도 작업으로 진행한다.

### Non-functional

- 간결하고 실행 가능한 지침이어야 하며, 생태계의 일반적 관례와 일치해야 한다.
- Gradle/Next.js와 궁합이 좋고 유지 비용이 낮은 도구를 우선한다.
- 점진적 도입이 가능해야 하며 초기 개발을 막지 않는다.

## 3. API Design (Draft)

- 문서 작업으로 런타임 API는 없다.

## 4. Domain Model (Draft)

- `docs/spec/v1.0.md`와 정합성을 맞춘다.
- 모든 엔티티는 공통 필드를 가진 `BaseEntity`를 상속한다.
  - `UUID id`
  - `LocalDateTime createdAt`
  - `LocalDateTime modifiedAt`
  - `@MappedSuperclass` + Spring Data JPA Auditing (`@CreatedDate`, `@LastModifiedDate`, `@EntityListeners(AuditingEntityListener.class)`).
  - ID 생성: Hibernate 6 `@UuidGenerator` (애플리케이션 측 생성).
  - 저장 타입: `BINARY(16)` 권장 (`@Column(columnDefinition = "BINARY(16)")`).

## 5. TDD Plan

- 실행 가능한 코드가 아닌 문서 산출물이므로 리뷰/승인으로 검증한다.

## 6. 제안 표준 (Draft)

### 6.1 백엔드 (Java 21 + Spring Boot 3.5)

- 패키지 구조
  - 최상위: `global`, `domain`
    - `global`: 설정, 보안, 에러, 공통 유틸 등 횡단 관심사
    - `domain.<feature>`
      - `web`: 컨트롤러, API DTO
      - `application`: 서비스/유스케이스, 트랜잭션 조율
      - `model`: 엔티티, 값 객체, 애그리거트
      - `repository`: Spring Data 리포지토리
- 네이밍
  - 클래스 PascalCase, 메서드/필드 camelCase, 상수 UPPER_SNAKE_CASE
  - 접미사: `Controller`, `Service`, `Repository`
- 엔티티/JPA
  - 비즈니스 로직 과다 포함 금지, 필요한 곳에는 값 객체 사용
  - `equals/hashCode`는 필요 시 비즈니스 키 기반, 아니면 식별자 기반(영속 이후)
  - 양방향 연관은 필요한 경우에만 사용
  - 모든 엔티티는 BaseEntity 정책을 따른다(위 4장 참고)
- Lombok
  - `@Getter`, `@Builder`, `@RequiredArgsConstructor` 정도만 허용, 엔티티에는 `@Data` 금지
- DTO/검증
  - 요청/응답 DTO는 가능하면 `record`로 정의
  - `jakarta.validation` + 메서드 검증 사용
- 예외/응답
  - `@ControllerAdvice`로 중앙 집중 처리, 공통 에러 포맷은 추후 API 스펙에서 정의
- 테스트
  - JUnit5, 네이밍 `shouldDoX_whenY()`
  - 도메인/서비스 단위 테스트, 웹 슬라이스 MockMvc, 리포지토리/트랜잭션 통합 테스트
- 포매팅/정적 분석(제안)
  - Spotless + Google Java Format
  - 최소 Checkstyle, 필요 시 Error Prone 검토

### 6.2 프런트엔드 (Next.js 16 + React 19 + TS)

- 프로젝트 레이아웃
  - App Router 관례, 기능별 디렉터리에 컴포넌트/훅/테스트를 모은다.
  - 파일명 `kebab-case.tsx/ts`, 컴포넌트는 PascalCase
- TypeScript
  - `strict` 모드, `any` 대신 `unknown`, 묵시적 `any` 금지
  - 상태는 구분 합집합, readonly/불변 패턴 선호
- React
  - 함수형 컴포넌트 + 훅, 커스텀 훅 파일명 `useXxx.ts`
  - 렌더 중 부수 효과 금지, 필요 시 메모이제이션은 프로파일링 근거가 있을 때만
  - 가능하면 서버 컴포넌트 활용, 상호작용 시 클라이언트 컴포넌트
- 스타일링
  - Tailwind 우선, 재사용 패턴은 컴포넌트로 추출, 불필요한 임의 값 지양
- 테스트
  - React Testing Library 중심, 스냅샷 남용 금지, 의미 있는 곳에 API 훅/라우팅 통합 테스트
- 포매팅/린트(제안)
  - ESLint(typescript-eslint + Next 권장 설정) + Prettier

## 7. 산출물

- `docs/standards/java-style.md`
- `docs/standards/ts-react-style.md`
- 포매터/린터 도입은 승인 후 별도 작업으로 추가한다.

## 8. 승인 기준

- 백엔드/프런트엔드 스타일 문서가 존재하며 명확한 규칙/예시를 제공한다.
- 네이밍, 구조, 테스트, 포매팅/린트 권장 사항을 모두 다룬다.
- 커밋/브랜치 규칙, Issue/PR 템플릿 등은 범위에서 제외된다(별도 작업으로 처리).
- 사용자가 본 계획을 검토·승인한다.
