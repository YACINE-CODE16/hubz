package com.hubz.application.service;

import com.hubz.application.dto.request.CreateMessageRequest;
import com.hubz.application.dto.request.UpdateMessageRequest;
import com.hubz.application.dto.response.ChatMessageResponse;
import com.hubz.application.port.out.MessageRepositoryPort;
import com.hubz.application.port.out.TeamRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.MessageNotFoundException;
import com.hubz.domain.exception.TeamNotFoundException;
import com.hubz.domain.model.Message;
import com.hubz.domain.model.Team;
import com.hubz.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamChatService Unit Tests")
class TeamChatServiceTest {

    @Mock
    private MessageRepositoryPort messageRepository;

    @Mock
    private TeamRepositoryPort teamRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private TeamChatService teamChatService;

    private UUID teamId;
    private UUID userId;
    private UUID organizationId;
    private UUID messageId;
    private Team testTeam;
    private User testUser;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        testTeam = Team.builder()
                .id(teamId)
                .name("Dev Team")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(userId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .profilePhotoUrl("photos/john.jpg")
                .build();

        testMessage = Message.builder()
                .id(messageId)
                .teamId(teamId)
                .userId(userId)
                .content("Hello team!")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Send Message Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should successfully send a message")
        void shouldSendMessage() {
            // Given
            CreateMessageRequest request = CreateMessageRequest.builder()
                    .content("Hello team!")
                    .build();

            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            ChatMessageResponse response = teamChatService.sendMessage(teamId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("Hello team!");
            assertThat(response.getTeamId()).isEqualTo(teamId);
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getAuthorName()).isEqualTo("John Doe");
            assertThat(response.getAuthorProfilePhotoUrl()).isEqualTo("photos/john.jpg");
            assertThat(response.isDeleted()).isFalse();
            assertThat(response.isEdited()).isFalse();
            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("Should set correct fields when saving message")
        void shouldSetCorrectFieldsWhenSaving() {
            // Given
            CreateMessageRequest request = CreateMessageRequest.builder()
                    .content("Test message content")
                    .build();

            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            when(messageRepository.save(messageCaptor.capture())).thenReturn(testMessage);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            teamChatService.sendMessage(teamId, request, userId);

            // Then
            Message savedMessage = messageCaptor.getValue();
            assertThat(savedMessage.getTeamId()).isEqualTo(teamId);
            assertThat(savedMessage.getUserId()).isEqualTo(userId);
            assertThat(savedMessage.getContent()).isEqualTo("Test message content");
            assertThat(savedMessage.isDeleted()).isFalse();
            assertThat(savedMessage.getCreatedAt()).isNotNull();
            assertThat(savedMessage.getEditedAt()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when team not found")
        void shouldThrowWhenTeamNotFound() {
            // Given
            UUID nonExistentTeamId = UUID.randomUUID();
            CreateMessageRequest request = CreateMessageRequest.builder()
                    .content("Hello")
                    .build();

            when(teamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> teamChatService.sendMessage(nonExistentTeamId, request, userId))
                    .isInstanceOf(TeamNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when user has no access to organization")
        void shouldThrowWhenNoAccess() {
            // Given
            CreateMessageRequest request = CreateMessageRequest.builder()
                    .content("Hello")
                    .build();

            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doThrow(AccessDeniedException.notMember())
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> teamChatService.sendMessage(teamId, request, userId))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Get Messages Tests")
    class GetMessagesTests {

        @Test
        @DisplayName("Should return paginated messages")
        void shouldReturnPaginatedMessages() {
            // Given
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            Page<Message> messagePage = new PageImpl<>(List.of(testMessage));
            when(messageRepository.findByTeamIdOrderByCreatedAtDesc(eq(teamId), any(Pageable.class)))
                    .thenReturn(messagePage);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            Page<ChatMessageResponse> result = teamChatService.getMessages(teamId, 0, 50, userId);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getContent()).isEqualTo("Hello team!");
            assertThat(result.getContent().get(0).getAuthorName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should return empty page when no messages exist")
        void shouldReturnEmptyPageWhenNoMessages() {
            // Given
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            Page<Message> emptyPage = new PageImpl<>(List.of());
            when(messageRepository.findByTeamIdOrderByCreatedAtDesc(eq(teamId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            Page<ChatMessageResponse> result = teamChatService.getMessages(teamId, 0, 50, userId);

            // Then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should cap page size to maximum 100")
        void shouldCapPageSizeToMax() {
            // Given
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            Page<Message> emptyPage = new PageImpl<>(List.of());
            when(messageRepository.findByTeamIdOrderByCreatedAtDesc(eq(teamId), pageableCaptor.capture()))
                    .thenReturn(emptyPage);

            // When
            teamChatService.getMessages(teamId, 0, 500, userId);

            // Then
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should throw exception when team not found")
        void shouldThrowWhenTeamNotFoundOnGetMessages() {
            // Given
            UUID nonExistentTeamId = UUID.randomUUID();
            when(teamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> teamChatService.getMessages(nonExistentTeamId, 0, 50, userId))
                    .isInstanceOf(TeamNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Edit Message Tests")
    class EditMessageTests {

        @Test
        @DisplayName("Should successfully edit own message")
        void shouldEditOwnMessage() {
            // Given
            UpdateMessageRequest request = UpdateMessageRequest.builder()
                    .content("Updated content")
                    .build();

            Message updatedMessage = Message.builder()
                    .id(messageId)
                    .teamId(teamId)
                    .userId(userId)
                    .content("Updated content")
                    .deleted(false)
                    .createdAt(testMessage.getCreatedAt())
                    .editedAt(LocalDateTime.now())
                    .build();

            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(messageRepository.save(any(Message.class))).thenReturn(updatedMessage);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            ChatMessageResponse response = teamChatService.editMessage(messageId, request, userId);

            // Then
            assertThat(response.getContent()).isEqualTo("Updated content");
            assertThat(response.isEdited()).isTrue();
            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("Should throw exception when editing another user's message")
        void shouldThrowWhenEditingOthersMessage() {
            // Given
            UUID differentUserId = UUID.randomUUID();
            UpdateMessageRequest request = UpdateMessageRequest.builder()
                    .content("Updated content")
                    .build();

            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));

            // When & Then
            assertThatThrownBy(() -> teamChatService.editMessage(messageId, request, differentUserId))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should throw exception when message not found")
        void shouldThrowWhenMessageNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            UpdateMessageRequest request = UpdateMessageRequest.builder()
                    .content("Updated content")
                    .build();

            when(messageRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> teamChatService.editMessage(nonExistentId, request, userId))
                    .isInstanceOf(MessageNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when editing a deleted message")
        void shouldThrowWhenEditingDeletedMessage() {
            // Given
            Message deletedMessage = Message.builder()
                    .id(messageId)
                    .teamId(teamId)
                    .userId(userId)
                    .content("Deleted content")
                    .deleted(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            UpdateMessageRequest request = UpdateMessageRequest.builder()
                    .content("Updated content")
                    .build();

            when(messageRepository.findById(messageId)).thenReturn(Optional.of(deletedMessage));

            // When & Then
            assertThatThrownBy(() -> teamChatService.editMessage(messageId, request, userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot edit a deleted message");
        }

        @Test
        @DisplayName("Should set editedAt timestamp when editing")
        void shouldSetEditedAtTimestamp() {
            // Given
            UpdateMessageRequest request = UpdateMessageRequest.builder()
                    .content("Updated content")
                    .build();

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(messageRepository.save(messageCaptor.capture())).thenReturn(testMessage);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            teamChatService.editMessage(messageId, request, userId);

            // Then
            Message savedMessage = messageCaptor.getValue();
            assertThat(savedMessage.getEditedAt()).isNotNull();
            assertThat(savedMessage.getContent()).isEqualTo("Updated content");
        }
    }

    @Nested
    @DisplayName("Delete Message Tests")
    class DeleteMessageTests {

        @Test
        @DisplayName("Should successfully soft-delete own message")
        void shouldSoftDeleteOwnMessage() {
            // Given
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            when(authorizationService.isOrganizationAdmin(organizationId, userId)).thenReturn(false);

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            when(messageRepository.save(messageCaptor.capture())).thenReturn(testMessage);

            // When
            teamChatService.deleteMessage(messageId, userId);

            // Then
            Message saved = messageCaptor.getValue();
            assertThat(saved.isDeleted()).isTrue();
            assertThat(saved.getContent()).isEqualTo("Ce message a ete supprime.");
            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("Should allow admin to delete any message")
        void shouldAllowAdminToDeleteAnyMessage() {
            // Given
            UUID adminId = UUID.randomUUID();
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            when(authorizationService.isOrganizationAdmin(organizationId, adminId)).thenReturn(true);
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

            // When
            teamChatService.deleteMessage(messageId, adminId);

            // Then
            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("Should throw exception when non-author non-admin tries to delete")
        void shouldThrowWhenNotAuthorOrAdmin() {
            // Given
            UUID differentUserId = UUID.randomUUID();
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            when(authorizationService.isOrganizationAdmin(organizationId, differentUserId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> teamChatService.deleteMessage(messageId, differentUserId))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should throw exception when message not found on delete")
        void shouldThrowWhenMessageNotFoundOnDelete() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(messageRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> teamChatService.deleteMessage(nonExistentId, userId))
                    .isInstanceOf(MessageNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Message Count Tests")
    class GetMessageCountTests {

        @Test
        @DisplayName("Should return correct message count")
        void shouldReturnCorrectMessageCount() {
            // Given
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(messageRepository.countByTeamId(teamId)).thenReturn(42);

            // When
            int count = teamChatService.getMessageCount(teamId, userId);

            // Then
            assertThat(count).isEqualTo(42);
        }

        @Test
        @DisplayName("Should return zero when no messages")
        void shouldReturnZeroWhenNoMessages() {
            // Given
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(messageRepository.countByTeamId(teamId)).thenReturn(0);

            // When
            int count = teamChatService.getMessageCount(teamId, userId);

            // Then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should return Unknown User when user not found")
        void shouldReturnUnknownUserWhenUserNotFound() {
            // Given
            CreateMessageRequest request = CreateMessageRequest.builder()
                    .content("Hello")
                    .build();

            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When
            ChatMessageResponse response = teamChatService.sendMessage(teamId, request, userId);

            // Then
            assertThat(response.getAuthorName()).isEqualTo("Unknown User");
            assertThat(response.getAuthorProfilePhotoUrl()).isNull();
        }

        @Test
        @DisplayName("Should hide author info for deleted messages")
        void shouldHideAuthorInfoForDeletedMessages() {
            // Given
            Message deletedMessage = Message.builder()
                    .id(messageId)
                    .teamId(teamId)
                    .userId(userId)
                    .content("Ce message a ete supprime.")
                    .deleted(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            Page<Message> messagePage = new PageImpl<>(List.of(deletedMessage));
            when(messageRepository.findByTeamIdOrderByCreatedAtDesc(eq(teamId), any(Pageable.class)))
                    .thenReturn(messagePage);

            // When
            Page<ChatMessageResponse> result = teamChatService.getMessages(teamId, 0, 50, userId);

            // Then
            ChatMessageResponse response = result.getContent().get(0);
            assertThat(response.isDeleted()).isTrue();
            assertThat(response.getAuthorName()).isEqualTo("Unknown User");
            // Deleted messages should not trigger a user lookup
            verify(userRepository, never()).findById(any());
        }
    }
}
