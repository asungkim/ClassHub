# Java + Spring 스타일 가이드

이 문서는 Java 21 + Spring Boot 3.5 백엔드 개발 표준을 정의한다.

## 1. 아키텍처 및 패키징

- 최상위 패키지
  - `global`: 설정, 보안, 에러 처리, 공통 유틸 등 횡단 관심사
  - `domain`: 기능 중심 코드
- `domain.<feature>` 하위 구조
  - `domain.<feature>.web`: 컨트롤러, 요청/응답 DTO
  - `domain.<feature>.application`: 서비스/유스케이스, 트랜잭션 조율
  - `domain.<feature>.model`: 엔티티, 값 객체, 애그리거트
  - `domain.<feature>.repository`: Spring Data 리포지토리
- 네이밍
  - 클래스: PascalCase / 메서드·필드: camelCase / 상수: UPPER_SNAKE_CASE
  - 접미사: `XxxController`, `XxxService`, `XxxRepository`

## 2. BaseEntity 및 식별자 정책

- 모든 JPA 엔티티는 공통 필드를 가진 `BaseEntity`를 상속한다.
  - `UUID id`
  - `LocalDateTime createdAt`
  - `LocalDateTime modifiedAt`
- 구현
  - `@MappedSuperclass`
  - `@EntityListeners(AuditingEntityListener.class)`
  - `@Id @GeneratedValue @UuidGenerator`
  - `@CreatedDate`, `@LastModifiedDate`
  - UUID 열은 효율성을 위해 `BINARY(16)`으로 저장한다.
- 예시

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime modifiedAt;
}
```

- 감사 기능 활성화

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
```

## 3. 엔티티 & JPA

- 엔티티에 과도한 도메인 로직을 넣지 말고, 필요 시 값 객체를 활용한다.
- 연관관계
  - 단방향 선호, 반드시 필요할 때만 양방향
  - 다대다에서는 Cascade를 지양하고, 애그리거트 경계를 통해 명시적으로 관리한다.
- equals/hashCode
  - 변경 가능한 필드를 사용하지 않는다.
  - 영속화 이후에는 식별자 기반, 그 전에는 안정적인 비즈니스 키 기반으로 구현한다.

## 4. DTO & 검증

- 요청/응답 DTO를 분리하고, 불변성을 위해 가능하면 Java `record`를 사용한다.
- 입력 검증은 `jakarta.validation` 애너테이션 및 메서드 검증을 활용한다.
- 도메인 ↔ DTO 매핑은 application/web 레이어에서 수행하고, 컨트롤러는 얇게 유지한다.

## 5. 예외 및 API 에러

- `@ControllerAdvice`로 중앙집중 처리한다.
- 에러 응답은 `code/message/details` 구조를 유지한다(구체 포맷은 API 스펙 작업에서 정의).

## 6. 서비스 & 트랜잭션 규칙

- `domain.<feature>.application`에서 오케스트레이션 및 트랜잭션 경계를 관리한다.
- 컨트롤러(`web`)와 리포지토리에는 비즈니스 로직을 넣지 말고, 도메인 로직은 `model`, 조율은 `application`에 둔다.
- `@Transactional`은 서비스 경계에서 선언하고, 읽기 전용 여부를 명시한다.

## 7. 테스트 규칙

- 네이밍: `shouldDoX_whenY()` 형태
- 단위 테스트: 도메인, 서비스 (의존성 목킹)
- Web 슬라이스: MockMvc
- 통합 테스트: 리포지토리 + 트랜잭션 경계를 테스트컨테이너 또는 로컬 Docker DB로 실행

## 8. 포매팅 & 정적 분석

- 포매팅: Spotless + Google Java Format
- 정적 분석: 기본 Checkstyle, 필요 시 Error Prone 추가 검토
- CI에서 포맷/체크 실행(후속 작업에서 추가)

## 9. 기타

- 시간 처리: 요구사항에 따라 `LocalDate/LocalTime/LocalDateTime`을 사용하고, 인프라 레벨에서는 한국 표준시를 기준으로 맞춘다.
- API에서는 UUID를 문자열로 노출하고, DB에는 `BINARY(16)`으로 저장한다.
