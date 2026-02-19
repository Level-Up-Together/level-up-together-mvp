# ADR-005: Gradle Multi-Module Migration

**Date:** 2026-02-12
**Status:** Accepted (Implemented)
**Deciders:** Backend Team
**Branch:** `feature/gradle-multi-module`

## Context

기존 단일 Gradle 모듈 프로젝트(Java 729, Test 155 파일)가 성장하면서 다음 문제가 발생했습니다:

1. **경계 없는 의존성**: 어떤 코드든 global/, service/ 내 모든 클래스에 접근 가능하여 서비스 간 경계가 코드 수준에서 강제되지 않음
2. **느린 빌드**: 작은 변경에도 전체 코드가 재컴파일
3. **MSA 전환 준비 부족**: 서비스가 물리적으로 분리되어 있지 않아 추출이 어려움
4. **패키지 충돌 가능성**: DataSourceProperties 등이 여러 서비스 패키지에 분산되어 Split Package 문제 잠재

### 요구사항
- 컴파일 타임에 서비스 경계를 강제 (최소한 platform ↔ service 분리)
- 빌드 속도 개선 (병렬 빌드 활용)
- 패키지명 변경 없이 전환 (리스크 최소화)
- 기존 1831 테스트 전수 통과

## Decision

**5개 Gradle 모듈**로 전환합니다: `platform:kernel`, `platform:infra`, `platform:saga`, `service`, `app`

원래 17개 모듈(서비스별 독립)을 계획했으나, **서비스 간 순환 의존성 4쌍**이 발견되어 서비스 모듈은 단일 모듈(multi-srcDirs)로 유지합니다.

### Final Module Structure

```
level-up-together-mvp/
├── settings.gradle                    # 5 modules
├── build.gradle                       # Root: common settings, BOM
├── platform/
│   ├── kernel/build.gradle            # Pure types (~80 files)
│   ├── infra/build.gradle             # Spring infra (~130 files)
│   └── saga/build.gradle              # Saga framework (~16 files)
├── service/build.gradle               # 12 services (multi-srcDirs)
│   ├── shared-test/                   # Shared test utilities
│   ├── user-service/src/main/java/
│   ├── guild-service/src/main/java/
│   └── ... (12 directories)
└── app/build.gradle                   # Bootstrap + DGS + JaCoCo
```

### Dependency Graph

```
kernel ← infra ← saga
            ↓
         service (depends on kernel + infra + saga)
            ↓
          app (depends on everything)
```

### Why 5 Modules Instead of 17

서비스 간 순환 의존성이 Gradle 모듈 독립 분리를 차단합니다:

| 순환 쌍 | 방향 | 파일 수 |
|---------|------|---------|
| user ↔ guild | 양방향 | 2+3 files |
| user ↔ gamification | 양방향 | 7+5 files |
| user ↔ support | 양방향 | 1+2 files |
| guild ↔ gamification | 양방향 | 3+2 files |

**해결 전략**: 디렉토리로 논리적 경계를 유지하되, `sourceSets.main.java.srcDirs`로 하나의 컴파일 단위로 구성. 순환 의존 제거 후 서비스 모듈 독립 분리를 진행.

### Platform Module Design

**kernel** — 순수 타입 (Spring 최소 의존):
- ApiResult, ApiStatus, CustomException, domain events, enums, audit entities
- MSA 전환 시 Maven 공유 라이브러리로 배포 예정
- `java-test-fixtures` plugin으로 TestReflectionUtils 공유

**infra** — Spring 인프라:
- Security (JWT, OAuth2), Redis, DataSource configs (9개 통합), messaging
- Translation, profanity detection, rate limiting, interceptors
- 모든 서비스 DataSourceProperties를 `global.config.datasource.properties`로 통합

**saga** — opt-in 모듈:
- SagaOrchestrator, AbstractSagaStep 등 saga 프레임워크
- SagaDataSourceConfig + SagaDataSourceProperties
- mission-service만 의존

### Test Distribution (1831 tests)

| Module | Tests | Content |
|--------|-------|---------|
| kernel | 39 | util tests |
| infra | 168 | resolver, validation, profanity, crypto, translation |
| saga | 29 | saga framework tests |
| service | 1583 | all service unit + controller tests |
| app | 12 | ApplicationTests, benchmark |

**Shared test utilities:**
- `kernel/src/testFixtures/` → `TestReflectionUtils` (59+ users, shared via `java-test-fixtures`)
- `service/shared-test/` → `ControllerTestConfig`, `BaseTestController`, `MockUtil`, `TestApplication`

### Key Implementation Details

1. **DataSourceProperties 통합** (Step 0): 9개 서비스별 Properties를 `platform/infra`의 `global.config.datasource.properties` 패키지로 이동. Split Package 방지.

2. **패키지명 불변**: 모든 Java 파일의 `package` 선언은 변경하지 않음 (DataSourceProperties 9개 제외). `import` 문도 최소 변경.

3. **TestApplication**: `@WebMvcTest`는 `@SpringBootApplication`이 클래스패스에 필요. service 모듈에 최소한의 `TestApplication` 클래스를 생성.

4. **Spring Cloud Config**: service 모듈 테스트에서 `ConfigDataMissingEnvironmentPostProcessor` 오류 방지를 위해 `service/shared-test/src/test/resources/application.yml`에 `spring.cloud.config.enabled: false` 설정.

## Consequences

### Positive
- **컴파일 경계**: platform ↔ service 간 의존성이 Gradle 레벨에서 강제됨
- **빌드 속도**: platform 모듈 변경 없이 service만 수정 시 증분 빌드 가능, `--parallel` 옵션 활용
- **MSA 준비**: kernel은 공유 라이브러리, saga는 opt-in으로 명확한 분리
- **테스트 격리**: 모듈별 독립 테스트 실행 가능 (예: `:platform:kernel:test`만 39 tests 실행)
- **코드 가독성**: 디렉토리 구조로 서비스별 소스 위치가 명확

### Negative
- **서비스 간 경계 미완성**: 순환 의존으로 service 모듈이 단일 → 서비스 간 import 제한 불가
- **빌드 설정 복잡도**: 5개 `build.gradle` + root 설정 관리 필요
- **테스트 유틸 관리**: `java-test-fixtures` + `shared-test` 이중 체계

### Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| 서비스 간 순환 의존 고착화 | Phase 2에서 순환 제거 → 서비스 모듈 독립 분리 |
| QueryDSL Q-class 충돌 | 각 모듈에 annotation processor 설정, `./gradlew clean` |
| Split Package (같은 패키지가 여러 모듈) | DataSourceProperties 통합으로 해결 |
| Spring ComponentScan 범위 | Application이 `io.pinkspider` 패키지 → 모든 하위 자동 스캔 |

## Alternatives Considered

### 1. 17 Modules (서비스별 독립)
- **Pros**: 서비스 간 컴파일 경계 완전 강제
- **Cons**: 순환 의존 4쌍이 Gradle 빌드를 차단
- **Rejected**: 순환 의존 제거 없이 구현 불가

### 2. global을 6~8개로 세분화 (core-domain, infra-redis, infra-security 등)
- **Pros**: 더 세밀한 의존성 제어
- **Cons**: 모듈 수 25+개로 폭증, 현재 규모(700파일)에 과도, infra-redis는 4파일짜리 모듈
- **Rejected**: 관리 비용 > 이익

### 3. global을 1개로 유지 (shared-all)
- **Pros**: 단순함
- **Cons**: 모든 서비스가 saga, messaging 등 불필요한 의존성도 전이
- **Rejected**: saga를 opt-in으로 분리하는 가치가 있음

### 4. Service API/Impl 분리 (service-user-api + service-user)
- **Pros**: 크로스-서비스 의존을 인터페이스로 제한
- **Cons**: 모듈 수 2배(26개), 모든 크로스-서비스 호출에 인터페이스 추출 필요
- **Rejected**: MSA 전환 직전이 아니면 과도

## Future Work

### Phase 2: 순환 의존 제거 → 서비스 모듈 독립 분리
1. user ↔ gamification 순환 제거 (가장 큰 7+5 files)
2. user ↔ guild, guild ↔ gamification 순환 제거
3. user ↔ support 순환 제거
4. 순환 제거 후 서비스별 독립 Gradle 모듈로 전환 (17 modules)

### Phase 3: MSA 전환
- kernel → Maven Central 배포 (공유 라이브러리)
- 서비스별 독립 Git 레포 또는 모노레포 유지
- `implementation project(':platform:kernel')` → `implementation 'io.pinkspider:platform-kernel:1.0.0'`

## References

- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [java-test-fixtures Plugin](https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures)
- ADR-001: Multi-Service Monolith Architecture
- ADR-003: Saga Pattern for Distributed Transactions

## Related ADRs

- ADR-001: Multi-Service Monolith Architecture (이 ADR의 기반 아키텍처)
- (Future) ADR-006: 순환 의존 제거 및 서비스 모듈 독립 분리
