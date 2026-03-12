package io.pinkspider.leveluptogethermvp.chatservice.application;

import static org.mockito.Mockito.verify;

import io.pinkspider.global.event.GuildMemberJoinedChatNotifyEvent;
import io.pinkspider.global.event.GuildMemberKickedChatNotifyEvent;
import io.pinkspider.global.event.GuildMemberLeftChatNotifyEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatEventListenerTest {

    @Mock
    private GuildChatService guildChatService;

    @InjectMocks
    private ChatEventListener chatEventListener;

    @Nested
    @DisplayName("handleMemberJoined 테스트")
    class HandleMemberJoinedTest {

        @Test
        @DisplayName("멤버 가입 이벤트 수신 시 notifyMemberJoin을 호출한다")
        void handleMemberJoined_success() {
            // given
            Long guildId = 1L;
            String nickname = "테스트유저";
            GuildMemberJoinedChatNotifyEvent event = new GuildMemberJoinedChatNotifyEvent(guildId, nickname);

            // when
            chatEventListener.handleMemberJoined(event);

            // then
            verify(guildChatService).notifyMemberJoin(guildId, nickname);
        }
    }

    @Nested
    @DisplayName("handleMemberLeft 테스트")
    class HandleMemberLeftTest {

        @Test
        @DisplayName("멤버 탈퇴 이벤트 수신 시 notifyMemberLeave를 호출한다")
        void handleMemberLeft_success() {
            // given
            Long guildId = 2L;
            String nickname = "탈퇴유저";
            GuildMemberLeftChatNotifyEvent event = new GuildMemberLeftChatNotifyEvent(guildId, nickname);

            // when
            chatEventListener.handleMemberLeft(event);

            // then
            verify(guildChatService).notifyMemberLeave(guildId, nickname);
        }
    }

    @Nested
    @DisplayName("handleMemberKicked 테스트")
    class HandleMemberKickedTest {

        @Test
        @DisplayName("멤버 추방 이벤트 수신 시 notifyMemberKick을 호출한다")
        void handleMemberKicked_success() {
            // given
            Long guildId = 3L;
            String nickname = "추방유저";
            GuildMemberKickedChatNotifyEvent event = new GuildMemberKickedChatNotifyEvent(guildId, nickname);

            // when
            chatEventListener.handleMemberKicked(event);

            // then
            verify(guildChatService).notifyMemberKick(guildId, nickname);
        }
    }
}
