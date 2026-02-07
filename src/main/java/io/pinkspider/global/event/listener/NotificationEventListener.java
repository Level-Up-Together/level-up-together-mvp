package io.pinkspider.global.event.listener;

import static io.pinkspider.global.config.AsyncConfig.EVENT_EXECUTOR;

import io.pinkspider.global.event.AchievementCompletedEvent;
import io.pinkspider.global.event.ContentReportedEvent;
import io.pinkspider.global.event.FeedCommentEvent;
import io.pinkspider.global.event.FriendRequestAcceptedEvent;
import io.pinkspider.global.event.FriendRequestEvent;
import io.pinkspider.global.event.FriendRequestProcessedEvent;
import io.pinkspider.global.event.FriendRequestRejectedEvent;
import io.pinkspider.global.event.GuildBulletinCreatedEvent;
import io.pinkspider.global.event.GuildChatMessageEvent;
import io.pinkspider.global.event.GuildCreationEligibleEvent;
import io.pinkspider.global.event.GuildInvitationEvent;
import io.pinkspider.global.event.GuildMissionArrivedEvent;
import io.pinkspider.global.event.MissionCommentEvent;
import io.pinkspider.global.event.TitleAcquiredEvent;
import io.pinkspider.leveluptogethermvp.guildservice.domain.entity.Guild;
import io.pinkspider.leveluptogethermvp.guildservice.domain.entity.GuildPost;
import io.pinkspider.leveluptogethermvp.guildservice.infrastructure.GuildPostRepository;
import io.pinkspider.leveluptogethermvp.guildservice.infrastructure.GuildRepository;
import io.pinkspider.leveluptogethermvp.notificationservice.application.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 관련 이벤트 리스너
 * 트랜잭션 커밋 후 비동기로 알림을 생성합니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final GuildRepository guildRepository;
    private final GuildPostRepository guildPostRepository;

    /**
     * 칭호 획득 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTitleAcquired(TitleAcquiredEvent event) {
        try {
            log.debug("칭호 획득 이벤트 처리: userId={}, titleName={}", event.userId(), event.titleName());
            notificationService.notifyTitleAcquired(
                event.userId(),
                event.titleId(),
                event.titleName(),
                event.rarity()
            );
        } catch (Exception e) {
            log.error("칭호 획득 알림 생성 실패: userId={}, error={}", event.userId(), e.getMessage(), e);
        }
    }

    /**
     * 업적 달성 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAchievementCompleted(AchievementCompletedEvent event) {
        try {
            log.debug("업적 달성 이벤트 처리: userId={}, achievementName={}", event.userId(), event.achievementName());
            notificationService.notifyAchievementCompleted(
                event.userId(),
                event.achievementId(),
                event.achievementName()
            );
        } catch (Exception e) {
            log.error("업적 달성 알림 생성 실패: userId={}, error={}", event.userId(), e.getMessage(), e);
        }
    }

    /**
     * 친구 요청 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequest(FriendRequestEvent event) {
        try {
            log.debug("친구 요청 이벤트 처리: from={}, to={}", event.userId(), event.targetUserId());
            notificationService.notifyFriendRequest(
                event.targetUserId(),
                event.requesterNickname(),
                event.friendshipId()
            );
        } catch (Exception e) {
            log.error("친구 요청 알림 생성 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 친구 요청 수락 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        try {
            log.debug("친구 요청 수락 이벤트 처리: accepter={}, requester={}", event.userId(), event.requesterId());
            notificationService.notifyFriendAccepted(
                event.requesterId(),
                event.accepterNickname(),
                event.friendshipId()
            );
        } catch (Exception e) {
            log.error("친구 수락 알림 생성 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 친구 요청 거절 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequestRejected(FriendRequestRejectedEvent event) {
        try {
            log.debug("친구 요청 거절 이벤트 처리: rejecter={}, requester={}", event.userId(), event.requesterId());
            notificationService.notifyFriendRejected(
                event.requesterId(),
                event.rejecterNickname(),
                event.friendshipId()
            );
        } catch (Exception e) {
            log.error("친구 거절 알림 생성 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 친구 요청 처리 완료 이벤트 (알림 삭제)
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFriendRequestProcessed(FriendRequestProcessedEvent event) {
        try {
            log.debug("친구 요청 알림 삭제: friendshipId={}", event.friendshipId());
            notificationService.deleteByReference("FRIEND_REQUEST", event.friendshipId());
        } catch (Exception e) {
            log.error("친구 요청 알림 삭제 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 길드 미션 도착 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGuildMissionArrived(GuildMissionArrivedEvent event) {
        try {
            log.debug("길드 미션 도착 이벤트 처리: missionId={}, memberCount={}",
                event.missionId(), event.memberIds().size());
            for (String memberId : event.memberIds()) {
                try {
                    notificationService.notifyGuildMissionArrived(
                        memberId,
                        event.missionTitle(),
                        event.missionId()
                    );
                } catch (Exception e) {
                    log.warn("길드 미션 알림 생성 실패: memberId={}, error={}", memberId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("길드 미션 알림 처리 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 길드 공지사항 등록 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGuildBulletinCreated(GuildBulletinCreatedEvent event) {
        try {
            log.debug("길드 공지사항 등록 이벤트 처리: postId={}, memberCount={}",
                event.postId(), event.memberIds().size());
            for (String memberId : event.memberIds()) {
                try {
                    notificationService.notifyGuildBulletin(
                        memberId,
                        event.guildName(),
                        event.guildId(),
                        event.postId(),
                        event.postTitle()
                    );
                } catch (Exception e) {
                    log.warn("길드 공지사항 알림 생성 실패: memberId={}, error={}", memberId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("길드 공지사항 알림 처리 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 피드 댓글 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFeedComment(FeedCommentEvent event) {
        try {
            // 자신의 글에 자신이 댓글 단 경우 알림 제외
            if (event.userId().equals(event.feedOwnerId())) {
                return;
            }
            log.debug("피드 댓글 이벤트 처리: commenter={}, feedOwner={}", event.userId(), event.feedOwnerId());
            notificationService.notifyCommentOnMyFeed(
                event.feedOwnerId(),
                event.commenterNickname(),
                event.feedId()
            );
        } catch (Exception e) {
            log.error("피드 댓글 알림 생성 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 미션 댓글 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionComment(MissionCommentEvent event) {
        try {
            log.debug("미션 댓글 이벤트 처리: commenter={}, missionCreator={}",
                event.userId(), event.missionCreatorId());
            notificationService.notifyCommentOnMyMission(
                event.missionCreatorId(),
                event.commenterNickname(),
                event.missionId(),
                event.missionTitle()
            );
        } catch (Exception e) {
            log.error("미션 댓글 알림 생성 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 길드 채팅 메시지 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGuildChatMessage(GuildChatMessageEvent event) {
        try {
            log.debug("길드 채팅 이벤트 처리: guildId={}, sender={}, memberCount={}",
                event.guildId(), event.userId(), event.memberIds().size());

            for (String memberId : event.memberIds()) {
                // 발송자 본인에게는 알림 안 보냄
                if (memberId.equals(event.userId())) {
                    continue;
                }
                try {
                    notificationService.notifyGuildChat(
                        memberId,
                        event.senderNickname(),
                        event.guildId(),
                        event.guildName(),
                        event.messageId(),
                        event.getPreviewContent()
                    );
                } catch (Exception e) {
                    log.warn("길드 채팅 알림 생성 실패: memberId={}, error={}", memberId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("길드 채팅 알림 처리 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 길드 창설 가능 레벨 도달 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGuildCreationEligible(GuildCreationEligibleEvent event) {
        try {
            log.debug("길드 창설 가능 이벤트 처리: userId={}, level={}", event.userId(), event.level());
            notificationService.notifyGuildCreationEligible(event.userId());
        } catch (Exception e) {
            log.error("길드 창설 가능 알림 생성 실패: userId={}, error={}", event.userId(), e.getMessage(), e);
        }
    }

    /**
     * 길드 초대 이벤트 처리
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGuildInvitation(GuildInvitationEvent event) {
        try {
            log.debug("길드 초대 이벤트 처리: inviteeId={}, guildId={}, invitationId={}",
                event.inviteeId(), event.guildId(), event.invitationId());
            notificationService.notifyGuildInvitation(
                event.inviteeId(),
                event.inviterNickname(),
                event.guildId(),
                event.guildName(),
                event.invitationId()
            );
        } catch (Exception e) {
            log.error("길드 초대 알림 생성 실패: inviteeId={}, error={}", event.inviteeId(), e.getMessage(), e);
        }
    }

    /**
     * 콘텐츠 신고 이벤트 처리
     * 신고 당한 유저에게 알림, 길드 관련 신고 시 길드 마스터에게도 알림
     */
    @Async(EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleContentReported(ContentReportedEvent event) {
        try {
            log.debug("콘텐츠 신고 이벤트 처리: targetType={}, targetId={}, targetUserId={}",
                event.targetType(), event.targetId(), event.targetUserId());

            String targetUserId = event.targetUserId();
            String targetTypeDescription = event.targetTypeDescription();

            // 1. 신고 당한 유저에게 알림
            if (targetUserId != null && !targetUserId.isBlank()) {
                try {
                    notificationService.notifyContentReported(targetUserId, targetTypeDescription);
                } catch (Exception e) {
                    log.warn("신고 대상 유저 알림 생성 실패: targetUserId={}, error={}", targetUserId, e.getMessage());
                }
            }

            // 2. 길드 관련 신고 시 길드 마스터에게도 알림
            notifyGuildMasterIfApplicable(event, targetUserId);

        } catch (Exception e) {
            log.error("콘텐츠 신고 알림 처리 실패: error={}", e.getMessage(), e);
        }
    }

    private void notifyGuildMasterIfApplicable(ContentReportedEvent event, String targetUserId) {
        String targetType = event.targetType();
        String guildMasterId = null;
        Long guildId = null;

        try {
            if ("GUILD".equals(targetType)) {
                guildId = Long.parseLong(event.targetId());
                Guild guild = guildRepository.findByIdAndIsActiveTrue(guildId).orElse(null);
                if (guild != null) {
                    guildMasterId = guild.getMasterId();
                }
            } else if ("GUILD_NOTICE".equals(targetType)) {
                Long postId = Long.parseLong(event.targetId());
                GuildPost post = guildPostRepository.findByIdAndIsDeletedFalse(postId).orElse(null);
                if (post != null && post.getGuild() != null) {
                    guildId = post.getGuild().getId();
                    guildMasterId = post.getGuild().getMasterId();
                }
            }

            // 길드 마스터에게 알림 (신고 당한 유저와 다른 경우에만)
            if (guildMasterId != null && !guildMasterId.equals(targetUserId)) {
                notificationService.notifyGuildContentReported(
                    guildMasterId, event.targetTypeDescription(), guildId);
            }
        } catch (NumberFormatException e) {
            log.warn("길드 관련 신고 처리 중 ID 파싱 실패: targetType={}, targetId={}", targetType, event.targetId());
        } catch (Exception e) {
            log.warn("길드 마스터 알림 생성 실패: targetType={}, error={}", targetType, e.getMessage());
        }
    }
}
