package io.pinkspider.leveluptogethermvp.missionservice.saga.steps;

import io.pinkspider.global.saga.SagaStep;
import io.pinkspider.global.saga.SagaStepResult;
import io.pinkspider.leveluptogethermvp.missionservice.saga.MissionCompletionContext;
import io.pinkspider.global.facade.GamificationQueryFacade;
import io.pinkspider.global.facade.dto.UserStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 통계 및 업적 업데이트 (일반 미션 + 고정 미션 통합)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateUserStatsStep implements SagaStep<MissionCompletionContext> {

    private final GamificationQueryFacade gamificationQueryFacadeService;

    @Override
    public String getName() {
        return "UpdateUserStats";
    }

    @Override
    public boolean isMandatory() {
        // 통계/업적 업데이트 실패는 미션 완료 자체를 실패시키지 않음
        return false;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SagaStepResult execute(MissionCompletionContext context) {
        String userId = context.getUserId();
        boolean isGuildMission = context.isGuildMission();

        log.debug("Updating user stats: userId={}, isGuildMission={}, pinned={}", userId, isGuildMission, context.isPinned());

        try {
            // 현재 상태 저장 (보상용)
            UserStatsDto currentStats = gamificationQueryFacadeService.getOrCreateUserStats(userId);
            UserStatsSnapshot snapshot = new UserStatsSnapshot(
                currentStats.totalMissionCompletions(),
                currentStats.totalGuildMissionCompletions(),
                currentStats.currentStreak(),
                currentStats.maxStreak()
            );
            context.addCompensationData(
                MissionCompletionContext.CompensationKeys.USER_STATS_BEFORE,
                snapshot);

            // 미션 완료 기록 (pinned 미션은 길드 미션이 아님 → isGuildMission()이 false 반환)
            gamificationQueryFacadeService.recordMissionCompletion(userId, isGuildMission);

            // 동적 Strategy 패턴으로 USER_STATS 관련 업적 체크
            gamificationQueryFacadeService.checkAchievementsByDataSource(userId, "USER_STATS");

            UserStatsDto updatedStats = gamificationQueryFacadeService.getOrCreateUserStats(userId);
            log.info("User stats updated: userId={}, totalCompletions={}, streak={}, pinned={}",
                userId, updatedStats.totalMissionCompletions(), updatedStats.currentStreak(), context.isPinned());

            return SagaStepResult.success("사용자 통계 업데이트 완료");

        } catch (Exception e) {
            log.warn("Failed to update user stats: userId={}, error={}", userId, e.getMessage());
            return SagaStepResult.failure("통계 업데이트 실패", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SagaStepResult compensate(MissionCompletionContext context) {
        String userId = context.getUserId();

        log.debug("Compensating user stats: userId={}", userId);

        try {
            UserStatsSnapshot snapshot = context.getCompensationData(
                MissionCompletionContext.CompensationKeys.USER_STATS_BEFORE,
                UserStatsSnapshot.class);

            if (snapshot != null) {
                // TODO: 통계 복원을 위한 facade 메서드 필요 (MSA 전환 시 추가)
                // UserStatsDto는 immutable record이므로 직접 변경 불가
                // 현재는 non-mandatory step이므로 스냅샷 기록만 로깅
                log.info("User stats compensate requested: userId={}, snapshot={}", userId, snapshot);
            }

            return SagaStepResult.success("사용자 통계 복원 완료");

        } catch (Exception e) {
            log.error("Failed to compensate user stats: userId={}, error={}", userId, e.getMessage());
            return SagaStepResult.failure("통계 복원 실패", e);
        }
    }

    /**
     * 통계 스냅샷 (보상용)
     */
    public record UserStatsSnapshot(
        int totalMissionCompletions,
        int totalGuildMissionCompletions,
        int currentStreak,
        int maxStreak
    ) {}
}
