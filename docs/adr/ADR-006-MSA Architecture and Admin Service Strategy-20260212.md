# ADR-006: MSA Architecture and Admin Service Strategy

**Date:** 2026-02-12
**Status:** Proposed
**Deciders:** Backend Team

## Context

Level Up Together 플랫폼의 전체 시스템 구성과 MSA 전환 전략을 결정해야 합니다.

### 현재 시스템 구성

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Clients                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────────┐  │
│  │ Product Web  │  │  Admin Web   │  │  React Native (WebView)      │  │
│  │  (Next.js)   │  │  (Next.js)   │  │  iOS / Android               │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┬───────────────┘  │
└─────────┼─────────────────┼──────────────────────────┼──────────────────┘
          │                 │                          │
          ▼                 ▼                          ▼
  ┌───────────────┐ ┌───────────────┐        ┌───────────────┐
  │ Product MVP   │ │ Admin Backend │        │ Product MVP   │
  │ Backend       │ │ (별도 프로세스)│        │ Backend       │
  │ :8443         │ │ :8444         │        │ :8443         │
  └───────┬───────┘ └───────┬───────┘        └───────────────┘
          │                 │
          ▼                 ▼
  ┌──────────────────────────────────┐
  │  PostgreSQL (서비스별 9개 DB)     │     ┌─────────┐  ┌─────────┐
  │  user_db, guild_db, mission_db,  │     │  Redis  │  │  Kafka  │
  │  feed_db, gamification_db,       │     └─────────┘  └─────────┘
  │  notification_db, admin_db,      │
  │  meta_db, saga_db               │     ┌──────────────────────┐
  └──────────────────────────────────┘     │ External Services    │
                                           │ OAuth2, Firebase,    │
  ┌──────────────────────────────────┐     │ Google Translation   │
  │ Config Server + Config Repo      │     └──────────────────────┘
  └──────────────────────────────────┘
```

### 현재 Admin Backend의 문제점

Admin backend(344 Java files)가 **7개 MVP 서비스 DB에 직접 JPA 접근**:

```
Admin Backend → user_db     (47 files: UserAdminController 등)
             → guild_db     (22 files: GuildAdminController 등)
             → mission_db   (24 files: MissionAdminController 등)
             → feed_db      (9 files: FeedAdminController 등)
             → gamification_db (78 files: Achievement, Title, Season, Event 등)
             → meta_db      (39 files: LevelConfig, ProfanityWord 등)
             → admin_db     (125 files: Admin 인증, 홈배너, 추천콘텐츠)
```

이 구조는 MSA에서 치명적 — 서비스가 분리되면 admin이 직접 DB에 접근할 수 없게 됩니다.

### 현재 MVP admin-service 모듈 (10 files)

MVP backend의 `service/admin-service/`는 **product 서비스들이 내부적으로 소비**하는 작은 모듈:

| Entity | 용도 | 소비자 |
|--------|------|--------|
| `HomeBanner` | 홈 배너 관리 | userservice(HomeService) |
| `FeaturedPlayer` | 추천 플레이어 | userservice(HomeService) |
| `FeaturedGuild` | 추천 길드 | guildservice(GuildQueryService) |
| `FeaturedFeed` | 추천 피드 | feedservice(FeedQueryService) |

### 핵심 차이점: Product vs Admin

| 구분 | Product Backend | Admin Backend |
|------|----------------|---------------|
| **사용자** | 일반 유저 (다수) | 운영팀 (소수) |
| **인증** | OAuth2 + JWT | Admin JWT + Role/Permission + IP Whitelist |
| **트래픽** | 높음, 스파이크 가능 | 낮음, 예측 가능 |
| **보안** | OAuth2 기반 | IP 화이트리스트 + RBAC |
| **포트** | :8443 | :8444 |
| **DB 접근** | 자기 서비스 DB만 | 모든 서비스 DB 직접 접근 |
| **특성** | 읽기 위주 | 읽기/쓰기 관리 |

## Decision

### Q1. API Gateway — Product AG와 Admin AG 별도 구성

**결정: 별도 API Gateway로 구성한다.**

```
                     ┌──────────────┐
  Product Clients ──▶│ Product AG   │──▶ Product Services
                     │ (Public)     │    (user, guild, mission, ...)
                     └──────────────┘

                     ┌──────────────┐
  Admin Clients   ──▶│ Admin AG     │──▶ Admin Service
                     │ (Internal)   │    (admin-specific endpoints)
                     └──────────────┘
```

**근거:**

| 관점 | Product AG | Admin AG |
|------|-----------|----------|
| **네트워크** | Public (인터넷 노출) | Internal (VPN/사내망) |
| **인증** | OAuth2 JWT 검증 | Admin JWT + IP Whitelist |
| **Rate Limiting** | 사용자별 제한 | 관대하거나 없음 |
| **CORS** | 특정 도메인 허용 | Admin 도메인만 |
| **라우팅** | `/api/v1/**` → 서비스 | `/api/admin/**` → admin-service |
| **스케일링** | 트래픽에 따라 auto-scale | 고정 인스턴스 |
| **장애 격리** | 트래픽 폭증이 admin에 영향 없음 | admin 장애가 product에 영향 없음 |

**대안 (단일 AG) 탈락 이유:**
- Admin IP 화이트리스트를 Public AG에 적용하면 라우팅 복잡도 증가
- 인증 로직이 완전히 다르므로 하나의 filter chain에서 분기하면 가독성 저하
- Product 트래픽 스파이크가 admin 접근에 영향을 줄 수 있음

> **구현 참고**: Spring Cloud Gateway 인스턴스 2개를 별도 배포하거나, 하나의 Gateway에서 VirtualHost(도메인) 기반으로 분리하는 방법도 가능. AWS에서는 ALB를 2개 구성하는 것이 가장 단순.

---

### Q2. Admin Service와 다른 서비스의 관계

**결정: Admin API 계층 + 서비스 API 호출 방식으로 전환한다.**

현재 admin backend의 직접 DB 접근을 **각 서비스의 Admin API 엔드포인트 호출**로 전환:

```
┌─ Before (현재) ──────────────────────────────────┐
│                                                   │
│  Admin Backend ──JPA──▶ user_db                   │
│                ──JPA──▶ guild_db                   │
│                ──JPA──▶ gamification_db            │
│                ──JPA──▶ ... (7개 DB 직접 접근)     │
│                                                   │
└───────────────────────────────────────────────────┘

┌─ After (MSA) ────────────────────────────────────┐
│                                                   │
│  Admin Service ──REST──▶ user-service             │
│    (admin_db)  ──REST──▶ guild-service            │
│                ──REST──▶ gamification-service      │
│                ──REST──▶ ... (서비스 API 호출)     │
│                                                   │
│  각 서비스에 /admin/** 엔드포인트 추가:            │
│    - user-service: /admin/users/**                │
│    - guild-service: /admin/guilds/**              │
│    - gamification-service: /admin/achievements/** │
│                                                   │
└───────────────────────────────────────────────────┘
```

**구체적 전환 전략:**

#### Phase 1: 각 서비스에 Admin API 추가

현재 admin backend의 서비스별 코드를 해당 서비스로 이전:

| Admin Backend 코드 | → 이전 대상 서비스 | 엔드포인트 예시 |
|--------------------|--------------------|----------------|
| `userservice/` (47 files) | user-service | `GET /admin/users`, `PATCH /admin/users/{id}/ban` |
| `guildservice/` (22 files) | guild-service | `GET /admin/guilds`, `DELETE /admin/guilds/{id}` |
| `missionservice/` (24 files) | mission-service | `GET /admin/missions`, `PATCH /admin/missions/{id}` |
| `feedservice/` (9 files) | feed-service | `GET /admin/feeds`, `DELETE /admin/feeds/{id}` |
| `gamificationservice/` (78 files) | gamification-service | `POST /admin/achievements`, `PUT /admin/titles/{id}` |
| `metaservice/` (39 files) | meta-service | `PUT /admin/level-config`, `POST /admin/profanity-words` |
| `adminservice/` (125 files) | admin-service (독립) | `POST /admin/auth/login`, `CRUD /admin/banners` |

#### Phase 2: Admin Service를 API Aggregation 레이어로 전환

```java
// Admin Service — BFF 패턴과 유사하게 다른 서비스 API를 조합
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final UserAdminClient userAdminClient;       // Feign → user-service
    private final GuildAdminClient guildAdminClient;     // Feign → guild-service
    private final FeedAdminClient feedAdminClient;       // Feign → feed-service

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        // 여러 서비스의 admin API를 병렬 호출하여 대시보드 데이터 집계
        var userStats = userAdminClient.getUserStats();
        var guildStats = guildAdminClient.getGuildStats();
        return new DashboardResponse(userStats, guildStats);
    }
}
```

#### Admin Service가 직접 소유하는 데이터 (admin_db)

| 도메인 | 현재 위치 | 비고 |
|--------|----------|------|
| Admin 계정 (인증/인가) | admin backend | Admin JWT, Role, Permission, Menu |
| 홈 배너 (HomeBanner) | MVP admin-service | Product 서비스들이 소비 → 아래 참고 |
| 추천 콘텐츠 (Featured*) | MVP admin-service | Product 서비스들이 소비 → 아래 참고 |
| 신고 관리 | admin backend | Report 처리 |
| 공지사항 관리 | admin backend + notice-service | 양쪽에 중복 존재 |

#### HomeBanner/FeaturedContent 소유권 문제

현재 MVP의 `admin-service` 모듈(10 files)은 Product 서비스들이 내부적으로 소비:
- `userservice/HomeService` → `HomeBannerService.getActiveBanners()`
- `guildservice/GuildQueryService` → `FeaturedContentQueryService.getActiveFeaturedGuildIds()`
- `feedservice/FeedQueryService` → `FeaturedContentQueryService.getActiveFeaturedFeedIds()`

**MSA 전환 시 선택지:**

| 옵션 | 방식 | 장점 | 단점 |
|------|------|------|------|
| **A. admin-service가 소유** | product 서비스들이 REST로 조회 | 데이터 관리 주체 명확 | product → admin 의존 발생 |
| **B. 각 서비스가 소유** | 배너→user, 추천길드→guild 등 | 서비스 자율성 | 분산 관리, admin이 여러 서비스에 write |
| **C. content-service 신설** | 콘텐츠 관리 전담 서비스 | 깔끔한 분리 | 작은 서비스 하나 더 증가 |

**권장: 옵션 A** — admin-service가 소유하되, product 서비스는 **이벤트 기반 캐시**로 소비:

```
Admin Service ──event──▶ Redis Cache ◀──read── Product Services
(HomeBanner 변경 시)     (banner:{type})        (HomeService 등)
```

이렇게 하면 product 서비스들이 admin-service에 동기 의존하지 않으면서도 최신 데이터를 활용 가능.

---

### Q3. Admin Service 코드 위치 — 별도 레포 유지

**결정: Admin backend는 별도 레포(`level-up-together-mvp-admin`)에 유지한다.**

MVP backend에 멀티 모듈로 합치지 않는다.

**근거:**

| 판단 기준 | MVP에 합침 | 별도 레포 유지 | 판정 |
|-----------|-----------|--------------|------|
| **배포 주기** | product 변경 시 admin 재배포 | 독립 배포 | **별도** |
| **인증 체계** | Security 설정 충돌 가능 | 각자 독립 | **별도** |
| **빌드 시간** | 전체 빌드 시간 증가 | 병렬 빌드 | **별도** |
| **팀 운영** | 코드 충돌 가능성 | 독립 개발 | **별도** |
| **DB 접근 패턴** | product는 자기 DB만, admin은 모든 DB | 혼재하면 경계 모호 | **별도** |
| **스케일링** | 완전히 다른 트래픽 패턴 | 독립 스케일 | **별도** |
| **보안 수준** | IP 화이트리스트 등 admin 전용 보안 | 분리가 안전 | **별도** |

**현재 MVP의 admin-service 모듈(10 files) 처리:**

MSA 전환 시 HomeBanner/FeaturedContent 엔티티는 admin-service(별도 레포)로 이전.
전환 전까지는 현재 MVP 내부에 유지하되, product 서비스들은 캐시 기반 조회로 전환 준비.

```
┌─ 전환 단계별 admin-service 위치 ──────────────────────────────────┐
│                                                                   │
│  현재:  MVP/service/admin-service (10 files, product 내부 소비)    │
│         + level-up-together-mvp-admin (344 files, 별도 레포)       │
│                                                                   │
│  MSA:   level-up-together-admin (통합, 독립 서비스)                │
│         = admin 인증/인가 + HomeBanner + FeaturedContent           │
│         + 각 서비스 admin API를 Feign으로 호출                      │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

---

## MSA 전환 목표 아키텍처

```
                    ┌──────────────────┐
                    │   Service        │
                    │   Discovery      │
                    │   (Eureka)       │
                    └────────┬─────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                  │
    ┌──────▼──────┐   ┌─────▼──────┐   ┌──────▼──────┐
    │ Product AG  │   │ Admin AG   │   │ Config      │
    │ (Public)    │   │ (Internal) │   │ Server      │
    └──────┬──────┘   └─────┬──────┘   └─────────────┘
           │                │
     ┌─────┼──────┬─────┐   │
     ▼     ▼      ▼     ▼   ▼
  ┌─────┐┌─────┐┌────┐┌──┐┌──────┐
  │user ││guild││miss││..││admin │
  │svc  ││svc  ││svc ││  ││svc   │
  └──┬──┘└──┬──┘└──┬─┘└──┘└──┬───┘
     │      │      │          │
     ▼      ▼      ▼          ▼
  ┌─────┐┌─────┐┌─────┐   ┌─────┐
  │user ││guild││miss │   │admin│
  │_db  ││_db  ││_db  │   │_db  │
  └─────┘└─────┘└─────┘   └─────┘

  ┌────────────────────────────────┐
  │  Shared Infrastructure         │
  │  Redis, Kafka/Redis Streams,   │
  │  CDN, Image Rekognition,       │
  │  CloudCache                    │
  └────────────────────────────────┘
```

### 서비스별 Admin API 엔드포인트 패턴

```
각 서비스는 두 종류의 API를 제공:

  user-service:
    /api/v1/users/**           ← Product AG → 일반 유저 접근
    /api/admin/users/**        ← Admin AG → 관리자 접근

  guild-service:
    /api/v1/guilds/**          ← Product AG
    /api/admin/guilds/**       ← Admin AG

  admin-service:
    /api/admin/auth/**         ← Admin AG only
    /api/admin/banners/**      ← Admin AG only
    /api/admin/dashboard/**    ← Admin AG (다른 서비스 집계)
```

**Admin API 보안**: Admin AG에서 JWT 검증 후 서비스 간 호출에는 내부 토큰(Service-to-Service) 사용. 각 서비스의 `/api/admin/**` 엔드포인트는 내부 네트워크에서만 접근 가능하도록 설정.

---

## AWS 인프라 구성 (참고)

```
┌─ AWS Infrastructure ───────────────────────────────────────────────┐
│                                                                     │
│  ┌─ Public Subnet ──────────────────────────────────────────────┐  │
│  │  ALB (Product) ──▶ Product AG (ECS/EKS)                      │  │
│  │  ALB (Admin)   ──▶ Admin AG (ECS/EKS) — IP 제한              │  │
│  │  CloudFront ──▶ S3 (이미지 CDN)                               │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌─ Private Subnet ─────────────────────────────────────────────┐  │
│  │  ECS/EKS Services: user, guild, mission, feed, ...           │  │
│  │  ECS/EKS Services: admin-service                             │  │
│  │  Config Server                                               │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌─ Data Layer ─────────────────────────────────────────────────┐  │
│  │  RDS PostgreSQL (서비스별 DB)                                  │  │
│  │  ElastiCache Redis / CloudCache                               │  │
│  │  MSK (Kafka) 또는 Redis Streams                               │  │
│  │  Rekognition (이미지 모더레이션)                                │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌─ Future ─────────────────────────────────────────────────────┐  │
│  │  Payment Service (PG 연동)                                    │  │
│  │  Subscription Service (구독 관리)                              │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Consequences

### Positive
- **장애 격리**: Product/Admin 트래픽 완전 분리
- **보안 강화**: Admin AG는 내부망/VPN으로 제한 가능
- **독립 배포**: Admin 변경이 Product에 영향 없음
- **MSA 정합성**: 각 서비스가 자기 데이터를 소유하는 원칙 유지
- **확장성**: Payment, Subscription 등 새 서비스 추가 용이

### Negative
- **Admin API 개발 비용**: 각 서비스에 admin 엔드포인트 추가 필요
- **AG 이중 운영 비용**: 2개의 Gateway 인스턴스
- **네트워크 홉 증가**: Admin → AG → Service (직접 DB 접근 대비 latency 증가)

### Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Admin API 개발량 (344 files 이전) | 점진적 전환 — 서비스별 admin API를 하나씩 추가 |
| Admin 조회 성능 저하 | 복잡한 조회는 CQRS read model 또는 admin용 집계 뷰 활용 |
| HomeBanner/Featured 의존 전환 | 이벤트 기반 캐시로 동기 의존 제거 |
| AG 단일 장애점 | AG 인스턴스 다중화, health check 설정 |

---

## 전환 로드맵 요약

| 단계 | 작업 | 상태 |
|------|------|------|
| **Phase 1** | MVP backend Gradle 멀티 모듈 전환 | ✅ 완료 |
| **Phase 2** | 순환 의존 제거 → 서비스 모듈 독립 분리 | 예정 |
| **Phase 3** | 각 서비스에 Admin API 엔드포인트 추가 | 예정 |
| **Phase 4** | Admin backend → admin-service 전환 (Feign 기반) | 예정 |
| **Phase 5** | API Gateway (Product/Admin) + Service Discovery 구성 | 예정 |
| **Phase 6** | AWS 배포 + CDN + CloudCache + Rekognition | 예정 |
| **Phase 7** | Payment/Subscription 서비스 추가 | 예정 |

## References

- ADR-001: Multi-Service Monolith Architecture
- ADR-005: Gradle Multi-Module Migration
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)
- [BFF Pattern](https://samnewman.io/patterns/architectural/bff/)
- [Backends for Frontends (Microsoft)](https://learn.microsoft.com/en-us/azure/architecture/patterns/backends-for-frontends)

## Related ADRs

- ADR-001: Multi-Service Monolith Architecture (기반 아키텍처)
- ADR-005: Gradle Multi-Module Migration (Phase 1 완료)
- (Future) ADR-007: 순환 의존 제거 전략
- (Future) ADR-008: AWS 인프라 구성
