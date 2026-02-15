# PLAN.md

> Source of Truth: [Notion - Platform 분리 + Admin 멀티모듈 + BFF 구성 플랜](https://www.notion.so/linkpark/Platform-Admin-BFF-3068c64c554381558c91ea67edf37a1d)

## 완료된 작업

- **Phase 1**: Platform 별도 레포지토리 분리 (2026-02-12)
- **platform/infra 모듈 제거**: 76개 파일 `service/src/`로 통합 + `includeBuild` 설정 (2026-02-15)

## 다음 작업

### Phase 3: MVP 서비스 간 순환 의존 제거

4개 순환 쌍을 제거하여 향후 서비스 모듈을 독립 Gradle 모듈로 분리할 수 있는 기반 마련.

| 순환 쌍 | A → B | B → A | 핵심 원인 |
|---------|-------|-------|----------|
| user ↔ guild | 3파일 (HomeService, MyPageService, UserAdminInternalService) | 4파일 (GuildInvitationService, GuildMemberService, GuildAdminInternalService, GuildQueryService) | user에서 길드 조회 / guild에서 유저 프로필 조회 |
| user ↔ gamification | 8파일 (UserProfileCacheService, MyPageService, HomeService 등) | 5파일 (UserExperienceService, SeasonRankingService 등) | user에서 레벨/칭호/스탯 조회 / gamification에서 유저 프로필 캐시 사용 |
| user ↔ support | 1파일 (MyPageService) | 2파일 (CustomerInquiryService, ReportService) | user에서 신고 상태 확인 / support에서 유저 정보 조회 |
| guild ↔ gamification | 3파일 (GuildService, GuildMemberService, GuildQueryService) | 2파일 (SeasonRankingService, GuildServiceCheckStrategy) | guild에서 레벨/칭호 조회 / gamification에서 길드 랭킹 조회 |

**핵심 패턴**: `UserProfileCacheService` 중심 순환
- `UserProfileCacheService` → `TitleService`, `UserExperienceService` (레벨/칭호 캐시)
- `UserExperienceService` → `UserProfileCacheService` (레벨업 시 캐시 무효화)

**해결 전략 (계획 중)**:
- 인터페이스 추출: 공통 조회 인터페이스를 platform 모듈에 정의, 각 서비스에서 구현
- 이벤트 기반: 레벨업/칭호변경 시 이벤트 발행 → 캐시 무효화
- Facade 패턴 강화: 기존 `GuildQueryFacadeService` 패턴 확대
