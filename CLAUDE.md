# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## HTTP API 테스트

`http/` 폴더에 IntelliJ HTTP Client 형식의 API 테스트 파일이 있습니다:

| 파일 | 설명 |
|------|------|
| `oauth-jwt.http` | OAuth2 로그인, JWT 토큰 관리, 모바일 소셜 로그인 |
| `mission.http` | 미션 CRUD, 참가자, 실행 추적, 캘린더 |
| `guild.http` | 길드 관리, 채팅, 게시판, 거점, DM |
| `activity-feed.http` | 피드, 좋아요, 댓글, 검색 |
| `friend.http` | 친구 요청/수락/거절/차단 |
| `mypage.http` | 프로필, 닉네임, 칭호 관리 |
| `achievement.http` | 업적, 칭호, 레벨 랭킹 |
| `attendance.http` | 출석 체크 |
| `notification.http` | 알림 관리, 읽음 처리 |
| `device-token.http` | FCM 토큰 등록/삭제 |
| `event.http` | 이벤트 API |
| `bff.http` | BFF 홈, 통합 검색, 시즌 |
| `home.http` | 홈 배너, 추천 콘텐츠 |
| `meta.http` | 메타데이터, 공통 코드 |
| `user-terms.http` | 약관 동의 |
| `user-experience.http` | 경험치, 레벨 |

환경 설정: `http/http-client.env.json`
```json
{
  "dev": { "baseUrl": "https://dev-api.level-up-together.com" },
  "local": { "baseUrl": "https://local.level-up-together.com:8443" },
  "test": { "baseUrl": "http://localhost:18080" }
}
```

## Build & Development Commands

```bash
# Build
./gradlew clean build

# Run tests
./gradlew test

# Run single test class
./gradlew test --tests "io.pinkspider.leveluptogethermvp.userservice.oauth.api.Oauth2ControllerTest"

# Run single test method
./gradlew test --tests "*.Oauth2ControllerTest.getOauth2LoginUri"

# Run application
./gradlew bootRun                                    # Default (port 8443)
./gradlew bootRun --args='--spring.profiles.active=test'  # Test profile (port 18080)

# Generate API documentation
./gradlew openapi3 && ./gradlew sortOpenApiJson && ./gradlew copySortedOpenApiJson

# Generate GraphQL classes from DGS schema
./gradlew generateJava
```

## Architecture Overview

**Multi-Service Monolith**: Spring Boot 3.4.5 application with service modules sharing a single deployment unit but using separate databases per service. Designed for future MSA migration with Saga pattern.

### Service Modules (`src/main/java/io/pinkspider/leveluptogethermvp/`)

| Service | Database | Purpose |
|---------|----------|---------|
| `userservice` | user_db | OAuth2 authentication (Google, Kakao, Apple), JWT tokens, profiles, friends, quests |
| `missionservice` | mission_db | Mission definition, progress tracking, Saga orchestration, mission book, daily mission instances (pinned missions) |
| `guildservice` | guild_db | Guild creation/management, members, experience/levels, bulletin board, chat, territory |
| `metaservice` | meta_db | Common codes, calendar holidays, Redis-cached metadata, level configuration |
| `feedservice` | feed_db | Activity feed, likes, comments, feed visibility management |
| `notificationservice` | notification_db | Push notifications, notification preferences, notification management |
| `adminservice` | admin_db | Home banners, featured content (players, guilds, feeds) |
| `gamificationservice` | gamification_db | Titles, achievements, user stats, experience/levels, attendance tracking, events, seasons |
| `bffservice` | - | Backend-for-Frontend API aggregation, unified search |
| `noticeservice` | - | Notice/announcement management |
| `supportservice` | - | Support/help functionality |
| `loggerservice` | MongoDB | Event logging with MongoDB and Kafka |
| `profanity` | meta_db | Profanity detection and filtering |

### Global Infrastructure (`src/main/java/io/pinkspider/global/`)

- **Multi-datasource**: Separate databases (user_db, mission_db, guild_db, meta_db, feed_db, notification_db, admin_db, gamification_db, saga_db) with Hikari pooling
- **Security**: JWT filter (`JwtAuthenticationFilter`), OAuth2 providers
- **Caching**: Redis with Lettuce client (two templates: `redisTemplateForString`, `redisTemplateForObject`)
- **Messaging**: Kafka topics (loggerTopic, httpLoggerTopic, alimTalkTopic, appPushTopic, emailTopic, userCommunicationTopic)
- **Events**: Spring Events for notification system and cross-service communication (`io.pinkspider.global.event`)
  - `@TransactionalEventListener(phase = AFTER_COMMIT)` for event handlers
  - Event records: `GuildJoinedEvent`, `GuildMasterAssignedEvent`, `GuildInvitationEvent`, `FriendRequestEvent`, `FriendRequestAcceptedEvent`, `TitleAcquiredEvent`, `AchievementCompletedEvent`, `MissionStateChangedEvent`, etc.
  - Event listeners: `AchievementEventListener`, `NotificationEventListener`, `MissionStateHistoryEventListener`
- **Caching**: Redis caching with `@Cacheable`/`@CacheEvict` annotations
  - `FriendCacheService` - 친구 목록 캐싱 (TTL 10분)
  - `UserProfileCacheService` - 유저 프로필 캐싱 (TTL 5분)
  - `TitleService` - 칭호 정보 캐싱 (TTL 5분)
  - `MissionCategoryService` - 카테고리 캐싱 (TTL 1시간)
- **Exception Handling**: Extend `CustomException` from `io.pinkspider.global.exception`
- **Encryption**: `CryptoConverter` for sensitive field encryption
- **Translation**: Google Translation API integration (`io.pinkspider.global.translation`)
- **Validation**: Profanity detection, Korean text normalization (`io.pinkspider.global.validation`)
- **Tracing**: Zipkin/Brave distributed tracing
- **Monitoring**: Actuator at `/showmethemoney`
- **Saga Pattern**: `io.pinkspider.global.saga` - 분산 트랜잭션 관리 (MSA 전환 대비)
- **Rate Limiting**: Resilience4j rate limiter (`io.pinkspider.global.config.RateLimiterConfig`)
- **Scheduled Tasks**: Spring `@Scheduled` for batch jobs (e.g., `DailyMissionInstanceScheduler`)

### Transaction Manager 주의사항

멀티 데이터소스 환경에서 `userTransactionManager`가 `@Primary`로 설정되어 있으므로, **각 서비스의 `@Transactional`에 명시적으로 트랜잭션 매니저를 지정해야 함**:

```java
// GuildService 예시
@Transactional(transactionManager = "guildTransactionManager")
public void updateGuild(...) { ... }

// MissionService 예시
@Transactional(transactionManager = "missionTransactionManager")
public void updateMission(...) { ... }
```

트랜잭션 매니저 매핑:
| Service | Transaction Manager |
|---------|---------------------|
| userservice | `userTransactionManager` (Primary) |
| missionservice | `missionTransactionManager` |
| guildservice | `guildTransactionManager` |
| metaservice | `metaTransactionManager` |
| feedservice | `feedTransactionManager` |
| notificationservice | `notificationTransactionManager` |
| adminservice | `adminTransactionManager` |
| gamificationservice | `gamificationTransactionManager` |
| saga | `sagaTransactionManager` |

### Service Layer Pattern

Each service module follows a consistent layered structure:
- `api/` - REST controllers returning `ApiResult<T>` wrapper
- `application/` - Business logic services with `@Transactional`
- `domain/` - Entities, DTOs, enums
- `infrastructure/` - JPA repositories
- `scheduler/` - Scheduled batch jobs (optional, e.g., `DailyMissionInstanceScheduler`)
- `saga/` - Saga orchestration steps (optional, e.g., `MissionCompletionSaga`)

### API Response Format

All REST endpoints return `ApiResult<T>` from `io.pinkspider.global.api`:
```java
{
  "code": "0000",      // ApiStatus code
  "message": "success",
  "value": { ... }     // Response payload
}
```

### Key Technologies

- GraphQL via Netflix DGS (schemas in `src/main/resources/schema/*.graphqls`)
- REST API with Spring REST Docs + OpenAPI 3.0
- QueryDSL for type-safe queries (generated in `build/generated/querydsl`)
- Resilience4j for circuit breaker
- Pact for contract testing

### Exception Handling

Custom exceptions should extend `CustomException` from `io.pinkspider.global.exception`:
```java
public class YourServiceException extends CustomException {
    public YourServiceException() {
        super("XXXXXX", "Error message");
    }
}
```

**ApiStatus 코드 할당 규칙** (6자리: 서비스 2자리 + 카테고리 2자리 + 일련번호 2자리):
| 서비스 | 코드 접두사 |
|--------|------------|
| global | 00 |
| bff-service | 01 |
| api-gateway | 02 |
| user-service | 03 |
| guild-service | 04 |
| mission-service | 05 |
| app-push-service | 06 |
| payment-service | 07 |
| meta-service | 08 |
| logger-service | 09 |
| stats-service | 10 |
| batch-service | 11 |
| gamification-service | 12 |
| feed-service | 13 |
| notification-service | 14 |

## Database Configuration

- **Production**: PostgreSQL with SSH tunnel support
- **Testing**: H2 in-memory with `MODE=PostgreSQL`
- **JPA**: `create-drop` in test, `validate` in production

Each service has its own datasource configuration in `io.pinkspider.global.config.datasource`.

## Testing

Tests exclude classes in `io/pinkspider/global/**`. JaCoCo 최소 커버리지: **70%**

### Test Fixtures
JSON fixtures in `src/test/resources/fixture/{servicename}/` are loaded via `MockUtil`:
```java
MockUtil.readJsonFileToClass("fixture/userservice/oauth/mockCreateJwtResponseDto.json", CreateJwtResponseDto.class);
```

### Controller Tests
Use `@WebMvcTest` with `ControllerTestConfig` for isolated controller testing:
```java
@WebMvcTest(controllers = YourController.class, excludeAutoConfiguration = {...})
@Import(ControllerTestConfig.class)
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
```

### Unit Tests (Service Layer)
Use `@ExtendWith(MockitoExtension.class)` for service layer unit testing:
```java
@ExtendWith(MockitoExtension.class)
class YourServiceTest {
    @Mock
    private YourRepository repository;

    @InjectMocks
    private YourService service;

    // Reflection을 사용하여 엔티티 ID 설정 (JPA 엔티티는 ID가 auto-generated)
    private void setEntityId(Entity entity, Long id) {
        try {
            Field idField = Entity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Integration Tests
Use `@SpringBootTest` with test profile for integration testing:
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional(transactionManager = "yourTransactionManager")
class YourIntegrationTest {
    @Autowired
    private YourService service;

    @Autowired
    private YourRepository repository;
}
```

## CI/CD

- Push to `main` → Production deployment with tests
- Push to `develop` → Dev deployment without tests
- Swagger docs updated on both environments
- Slack notifications on deployment status

## Configuration Profiles

- `application.yml` - Default configuration
- `application-test.yml` - H2 databases, test Kafka (port 18080)
- `application-unit-test.yml` - Unit test configuration
- `application-local.yml` - Config server integration
- `application-dev.yml` / `application-prod.yml` - Environment-specific

## 코드 컨벤션

- **API 필드명**: 프론트와 백엔드간 통신은 snake_case 사용
- **들여쓰기**: 4 spaces
- **DTO**: record 사용 권장
- **레이어 구조**: Controller → Service → Repository
- **테스트**: JUnit 5 + Mockito

## 관련 프로젝트

| 프로젝트 | 경로 |
|---------|------|
| Admin Backend | `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-mvp-admin` |
| Admin Frontend | `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-admin-frontend` |
| Product Frontend | `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-frontend` |
| SQL Scripts | `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-sql/queries` |
| Config Server | `/Users/pink-spider/Code/github/Level-Up-Together/config-repository` |
| React Native App | `/Users/pink-spider/Code/github/Level-Up-Together/LevelUpTogetherReactNative` |

## 자주 발생하는 이슈

### QueryDSL 빌드 오류
`Attempt to recreate a file for type Q*` 오류 발생 시:
```bash
./gradlew clean compileJava
```

### 트랜잭션 매니저 미지정 오류
데이터가 저장되지 않거나 조회되지 않는 경우, `@Transactional`에 올바른 트랜잭션 매니저가 지정되어 있는지 확인

### Integration Tests 실패
SSH 터널이나 외부 서비스 연결이 필요한 테스트는 로컬에서 실패할 수 있음. `@ActiveProfiles("test")` 확인

## Mission Service: Pinned Mission (고정 미션)

고정 미션은 Template-Instance 패턴으로 구현됩니다:

### DailyMissionInstance 엔티티
- 고정 미션(`isPinned=true`)은 `DailyMissionInstance` 엔티티를 사용
- 매일 자동 생성 (스케줄러: `DailyMissionInstanceScheduler`, cron: `0 5 0 * * *`)
- 미션 정보 스냅샷 저장 (미션 변경 시 과거 기록 보존)
- 피드와 1:1 관계 (`feedId` 필드)

### API 라우팅 (하위 호환성 유지)
```java
// MissionExecutionService에서 isPinned 체크 후 분기
if (isPinnedMission(missionId, userId)) {
    return dailyMissionInstanceService.startInstanceByMission(...);
}
```

### 관련 파일
- `domain/entity/DailyMissionInstance.java` - 일일 인스턴스 엔티티
- `application/DailyMissionInstanceService.java` - 인스턴스 서비스
- `scheduler/DailyMissionInstanceScheduler.java` - 배치 스케줄러
- `infrastructure/DailyMissionInstanceRepository.java` - 리포지토리

### 마이그레이션
```bash
# mission_db에서 실행
psql -d mission_db -f level-up-together-sql/queries/migration/20260122_create_daily_mission_instance.sql
```

## Guild Service: Guild Invitation (길드 초대)

비공개 길드를 위한 초대 시스템입니다.

### GuildInvitation 엔티티
- 비공개 길드 마스터/관리자가 다른 유저를 초대
- 초대 상태: `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `EXPIRED`
- 만료 시간 지원 (7일)
- 같은 카테고리의 다른 길드 가입자는 초대 불가

### API 엔드포인트
```
POST   /api/v1/guilds/{guildId}/invitations         - 초대 발송
GET    /api/v1/users/me/guild-invitations           - 내 대기중 초대 목록
POST   /api/v1/guild-invitations/{id}/accept        - 초대 수락
POST   /api/v1/guild-invitations/{id}/reject        - 초대 거절
DELETE /api/v1/guild-invitations/{id}               - 초대 취소 (마스터)
```

### 관련 파일
- `domain/entity/GuildInvitation.java` - 초대 엔티티
- `domain/enums/GuildInvitationStatus.java` - 초대 상태 Enum
- `application/GuildInvitationService.java` - 초대 서비스
- `api/GuildInvitationController.java` - REST 컨트롤러
- `infrastructure/GuildInvitationRepository.java` - 리포지토리

### 이벤트
- `GuildInvitationEvent`: 초대 발송 시 알림 이벤트 발행

### 마이그레이션
```bash
# guild_db에서 실행
psql -d guild_db -f level-up-together-sql/archive/queries/migration/20260124_create_guild_invitation.sql
```

## Saga Pattern: Mission Completion 예시

미션 완료 시 여러 서비스에 걸친 분산 트랜잭션을 Saga 패턴으로 관리합니다.

### Saga 구조
```
missionservice/saga/
├── MissionCompletionSaga.java        - Saga 오케스트레이터
├── MissionCompletionContext.java     - Saga 컨텍스트 (공유 데이터)
└── steps/
    ├── LoadMissionDataStep.java      - 미션 데이터 로드
    ├── CompleteExecutionStep.java    - 실행 완료 처리
    ├── UpdateParticipantProgressStep.java - 참가자 진행 업데이트
    ├── GrantUserExperienceStep.java  - 유저 경험치 지급
    ├── GrantGuildExperienceStep.java - 길드 경험치 지급
    ├── UpdateUserStatsStep.java      - 유저 통계 업데이트
    └── CreateFeedFromMissionStep.java - 피드 생성
```

### Saga Step 구현
```java
@Component
public class YourStep extends AbstractSagaStep<YourContext> {

    @Override
    public String getStepName() {
        return "YOUR_STEP";
    }

    @Override
    protected SagaStepResult executeInternal(YourContext context) {
        // 비즈니스 로직
        return SagaStepResult.success();
    }

    @Override
    protected SagaStepResult compensateInternal(YourContext context) {
        // 보상 트랜잭션 (롤백 로직)
        return SagaStepResult.success();
    }
}
```

## Event-Driven 패턴

### 이벤트 발행
```java
@Service
@RequiredArgsConstructor
public class YourService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(transactionManager = "yourTransactionManager")
    public void doSomething() {
        // 비즈니스 로직
        eventPublisher.publishEvent(new YourEvent(userId, data));
    }
}
```

### 이벤트 수신
```java
@Component
@RequiredArgsConstructor
public class YourEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(YourEvent event) {
        // 이벤트 처리 (다른 서비스 호출, 알림 발송 등)
    }
}
```

### 주요 이벤트 흐름
| 발행 서비스 | 이벤트 | 수신 리스너 | 처리 내용 |
|------------|--------|------------|----------|
| GuildService | `GuildJoinedEvent` | `AchievementEventListener` | 길드 가입 업적 체크 |
| GuildService | `GuildInvitationEvent` | `NotificationEventListener` | 초대 알림 발송 |
| FriendService | `FriendRequestAcceptedEvent` | `NotificationEventListener` | 친구 수락 알림 |
| GamificationService | `TitleAcquiredEvent` | `NotificationEventListener` | 칭호 획득 알림 |
| GamificationService | `AchievementCompletedEvent` | `NotificationEventListener` | 업적 달성 알림 |
| MissionService | `MissionStateChangedEvent` | `MissionStateHistoryEventListener` | 미션 상태 이력 저장 |