package io.pinkspider.leveluptogethermvp.guildservice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.pinkspider.leveluptogethermvp.guildservice.domain.dto.ChatMessageRequest;
import io.pinkspider.leveluptogethermvp.guildservice.domain.dto.ChatMessageResponse;
import io.pinkspider.leveluptogethermvp.guildservice.domain.entity.Guild;
import io.pinkspider.leveluptogethermvp.guildservice.domain.entity.GuildChatMessage;
import io.pinkspider.leveluptogethermvp.guildservice.domain.enums.ChatMessageType;
import io.pinkspider.leveluptogethermvp.guildservice.domain.enums.GuildVisibility;
import io.pinkspider.leveluptogethermvp.guildservice.infrastructure.GuildChatMessageRepository;
import io.pinkspider.leveluptogethermvp.guildservice.infrastructure.GuildMemberRepository;
import io.pinkspider.leveluptogethermvp.guildservice.infrastructure.GuildRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class GuildChatServiceTest {

    @Mock
    private GuildChatMessageRepository chatMessageRepository;

    @Mock
    private GuildRepository guildRepository;

    @Mock
    private GuildMemberRepository memberRepository;

    @InjectMocks
    private GuildChatService guildChatService;

    private Guild testGuild;
    private GuildChatMessage testMessage;
    private String testUserId;
    private String testNickname;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-id";
        testNickname = "테스터";

        testGuild = Guild.builder()
            .name("테스트 길드")
            .description("테스트 길드 설명")
            .visibility(GuildVisibility.PUBLIC)
            .masterId(testUserId)
            .maxMembers(50)
            .categoryId(1L)
            .build();
        setId(testGuild, Guild.class, 1L);

        testMessage = GuildChatMessage.createTextMessage(testGuild, testUserId, testNickname, "테스트 메시지");
        setId(testMessage, GuildChatMessage.class, 1L);
    }

    private <T> void setId(T entity, Class<T> clazz, Long id) {
        try {
            java.lang.reflect.Field idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("메시지 전송 테스트")
    class SendMessageTest {

        @Test
        @DisplayName("텍스트 메시지를 전송한다")
        void sendMessage_text_success() {
            // given
            ChatMessageRequest request = ChatMessageRequest.builder()
                .content("안녕하세요!")
                .build();

            when(guildRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testGuild));
            when(memberRepository.isActiveMember(1L, testUserId)).thenReturn(true);
            when(chatMessageRepository.save(any(GuildChatMessage.class))).thenAnswer(inv -> {
                GuildChatMessage msg = inv.getArgument(0);
                setId(msg, GuildChatMessage.class, 1L);
                return msg;
            });

            // when
            ChatMessageResponse response = guildChatService.sendMessage(1L, testUserId, testNickname, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("안녕하세요!");
            assertThat(response.getMessageType()).isEqualTo(ChatMessageType.TEXT);
            verify(chatMessageRepository).save(any(GuildChatMessage.class));
        }

        @Test
        @DisplayName("이미지 메시지를 전송한다")
        void sendMessage_image_success() {
            // given
            ChatMessageRequest request = ChatMessageRequest.builder()
                .content("이미지 설명")
                .imageUrl("https://example.com/image.jpg")
                .build();

            when(guildRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testGuild));
            when(memberRepository.isActiveMember(1L, testUserId)).thenReturn(true);
            when(chatMessageRepository.save(any(GuildChatMessage.class))).thenAnswer(inv -> {
                GuildChatMessage msg = inv.getArgument(0);
                setId(msg, GuildChatMessage.class, 1L);
                return msg;
            });

            // when
            ChatMessageResponse response = guildChatService.sendMessage(1L, testUserId, testNickname, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMessageType()).isEqualTo(ChatMessageType.IMAGE);
            assertThat(response.getImageUrl()).isEqualTo("https://example.com/image.jpg");
        }

        @Test
        @DisplayName("비멤버는 메시지를 전송할 수 없다")
        void sendMessage_nonMember_fail() {
            // given
            ChatMessageRequest request = ChatMessageRequest.builder()
                .content("안녕하세요!")
                .build();

            when(guildRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testGuild));
            when(memberRepository.isActiveMember(1L, testUserId)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> guildChatService.sendMessage(1L, testUserId, testNickname, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("길드 멤버만 채팅에 참여할 수 있습니다");
        }

        @Test
        @DisplayName("존재하지 않는 길드에 메시지 전송 시 예외 발생")
        void sendMessage_guildNotFound_fail() {
            // given
            ChatMessageRequest request = ChatMessageRequest.builder()
                .content("안녕하세요!")
                .build();

            when(guildRepository.findByIdAndIsActiveTrue(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> guildChatService.sendMessage(999L, testUserId, testNickname, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("길드를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("시스템 메시지 테스트")
    class SystemMessageTest {

        @Test
        @DisplayName("시스템 메시지를 전송한다")
        void sendSystemMessage_success() {
            // given
            when(guildRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testGuild));
            when(chatMessageRepository.save(any(GuildChatMessage.class))).thenAnswer(inv -> {
                GuildChatMessage msg = inv.getArgument(0);
                setId(msg, GuildChatMessage.class, 1L);
                return msg;
            });

            // when
            ChatMessageResponse response = guildChatService.sendSystemMessage(
                1L, ChatMessageType.SYSTEM_JOIN, "테스터님이 길드에 가입했습니다."
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMessageType()).isEqualTo(ChatMessageType.SYSTEM_JOIN);
        }

        @Test
        @DisplayName("멤버 가입 알림을 전송한다")
        void notifyMemberJoin_success() {
            // given
            when(guildRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testGuild));
            when(chatMessageRepository.save(any(GuildChatMessage.class))).thenAnswer(inv -> {
                GuildChatMessage msg = inv.getArgument(0);
                setId(msg, GuildChatMessage.class, 1L);
                return msg;
            });

            // when
            guildChatService.notifyMemberJoin(1L, "새멤버");

            // then
            verify(chatMessageRepository).save(any(GuildChatMessage.class));
        }

        @Test
        @DisplayName("멤버 탈퇴 알림을 전송한다")
        void notifyMemberLeave_success() {
            // given
            when(guildRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testGuild));
            when(chatMessageRepository.save(any(GuildChatMessage.class))).thenAnswer(inv -> {
                GuildChatMessage msg = inv.getArgument(0);
                setId(msg, GuildChatMessage.class, 1L);
                return msg;
            });

            // when
            guildChatService.notifyMemberLeave(1L, "탈퇴멤버");

            // then
            verify(chatMessageRepository).save(any(GuildChatMessage.class));
        }
    }

    @Nested
    @DisplayName("메시지 조회 테스트")
    class GetMessagesTest {

        @Test
        @DisplayName("메시지 목록을 조회한다")
        void getMessages_success() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<GuildChatMessage> messagePage = new PageImpl<>(List.of(testMessage), pageable, 1);

            when(memberRepository.isActiveMember(1L, testUserId)).thenReturn(true);
            when(chatMessageRepository.findByGuildIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(messagePage);

            // when
            Page<ChatMessageResponse> result = guildChatService.getMessages(1L, testUserId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("특정 시간 이후의 새 메시지를 조회한다")
        void getNewMessages_success() {
            // given
            LocalDateTime since = LocalDateTime.now().minusMinutes(5);

            when(memberRepository.isActiveMember(1L, testUserId)).thenReturn(true);
            when(chatMessageRepository.findNewMessages(1L, since)).thenReturn(List.of(testMessage));

            // when
            List<ChatMessageResponse> result = guildChatService.getNewMessages(1L, testUserId, since);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("특정 ID 이후의 메시지를 조회한다")
        void getMessagesAfterId_success() {
            // given
            when(memberRepository.isActiveMember(1L, testUserId)).thenReturn(true);
            when(chatMessageRepository.findMessagesAfterId(1L, 0L)).thenReturn(List.of(testMessage));

            // when
            List<ChatMessageResponse> result = guildChatService.getMessagesAfterId(1L, testUserId, 0L);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("비멤버는 메시지를 조회할 수 없다")
        void getMessages_nonMember_fail() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            when(memberRepository.isActiveMember(1L, testUserId)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> guildChatService.getMessages(1L, testUserId, pageable))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("길드 멤버만 채팅에 참여할 수 있습니다");
        }
    }

    @Nested
    @DisplayName("메시지 삭제 테스트")
    class DeleteMessageTest {

        @Test
        @DisplayName("본인 메시지를 삭제한다")
        void deleteMessage_byOwner_success() {
            // given
            when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(testMessage));

            // when
            guildChatService.deleteMessage(1L, 1L, testUserId);

            // then
            assertThat(testMessage.getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("길드 마스터가 다른 사람 메시지를 삭제할 수 있다")
        void deleteMessage_byMaster_success() {
            // given
            String otherUserId = "other-user-id";
            GuildChatMessage otherMessage = GuildChatMessage.createTextMessage(testGuild, otherUserId, "다른유저", "다른 메시지");
            setId(otherMessage, GuildChatMessage.class, 2L);

            when(chatMessageRepository.findById(2L)).thenReturn(Optional.of(otherMessage));

            // when
            guildChatService.deleteMessage(1L, 2L, testUserId); // masterId = testUserId

            // then
            assertThat(otherMessage.getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("다른 사람 메시지는 삭제할 수 없다")
        void deleteMessage_byOtherUser_fail() {
            // given
            String otherUserId = "other-user-id";

            // testGuild의 마스터를 다른 사람으로 설정
            Guild otherGuild = Guild.builder()
                .name("다른 길드")
                .masterId("another-master-id")
                .build();
            setId(otherGuild, Guild.class, 2L);

            GuildChatMessage otherMessage = GuildChatMessage.createTextMessage(otherGuild, "message-owner", "메시지소유자", "다른 메시지");
            setId(otherMessage, GuildChatMessage.class, 2L);

            when(chatMessageRepository.findById(2L)).thenReturn(Optional.of(otherMessage));

            // when & then
            assertThatThrownBy(() -> guildChatService.deleteMessage(2L, 2L, otherUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("본인 메시지 또는 길드 마스터만 삭제할 수 있습니다");
        }

        @Test
        @DisplayName("존재하지 않는 메시지 삭제 시 예외 발생")
        void deleteMessage_notFound_fail() {
            // given
            when(chatMessageRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> guildChatService.deleteMessage(1L, 999L, testUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("메시지를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("메시지 검색 테스트")
    class SearchMessagesTest {

        @Test
        @DisplayName("키워드로 메시지를 검색한다")
        void searchMessages_success() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<GuildChatMessage> messagePage = new PageImpl<>(List.of(testMessage), pageable, 1);

            when(memberRepository.isActiveMember(1L, testUserId)).thenReturn(true);
            when(chatMessageRepository.searchMessages(1L, "테스트", pageable)).thenReturn(messagePage);

            // when
            Page<ChatMessageResponse> result = guildChatService.searchMessages(1L, testUserId, "테스트", pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }
}
