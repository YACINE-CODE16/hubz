package com.hubz.application.service;

import com.hubz.application.dto.request.SendDirectMessageRequest;
import com.hubz.application.dto.request.UpdateDirectMessageRequest;
import com.hubz.application.dto.response.ConversationResponse;
import com.hubz.application.dto.response.DirectMessageResponse;
import com.hubz.application.dto.response.UnreadCountResponse;
import com.hubz.application.port.out.DirectMessageRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.DirectMessageNotFoundException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.DirectMessage;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DirectMessageService Unit Tests")
class DirectMessageServiceTest {

    @Mock
    private DirectMessageRepositoryPort directMessageRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DirectMessageService directMessageService;

    private UUID senderId;
    private UUID receiverId;
    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        receiverId = UUID.randomUUID();

        sender = User.builder()
                .id(senderId)
                .email("sender@test.com")
                .firstName("Alice")
                .lastName("Sender")
                .profilePhotoUrl("photo-sender.jpg")
                .build();

        receiver = User.builder()
                .id(receiverId)
                .email("receiver@test.com")
                .firstName("Bob")
                .lastName("Receiver")
                .profilePhotoUrl("photo-receiver.jpg")
                .build();
    }

    private DirectMessage buildMessage(UUID id, UUID senderId, UUID receiverId, String content, boolean read, boolean deleted) {
        return DirectMessage.builder()
                .id(id)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .read(read)
                .deleted(deleted)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== sendMessage ====================

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("should send a message successfully")
        void shouldSendMessageSuccessfully() {
            // Arrange
            SendDirectMessageRequest request = SendDirectMessageRequest.builder()
                    .receiverId(receiverId)
                    .content("Hello Bob!")
                    .build();

            DirectMessage saved = buildMessage(UUID.randomUUID(), senderId, receiverId, "Hello Bob!", false, false);

            when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
            when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
            when(directMessageRepository.save(any(DirectMessage.class))).thenReturn(saved);

            // Act
            DirectMessageResponse response = directMessageService.sendMessage(request, senderId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("Hello Bob!");
            assertThat(response.getSenderId()).isEqualTo(senderId);
            assertThat(response.getReceiverName()).isEqualTo("Bob Receiver");

            verify(directMessageRepository).save(any(DirectMessage.class));
            verify(notificationService).createNotification(
                    eq(receiverId), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should throw exception when sender not found")
        void shouldThrowWhenSenderNotFound() {
            // Arrange
            SendDirectMessageRequest request = SendDirectMessageRequest.builder()
                    .receiverId(receiverId)
                    .content("Hello!")
                    .build();

            when(userRepository.findById(senderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.sendMessage(request, senderId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(directMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when receiver not found")
        void shouldThrowWhenReceiverNotFound() {
            // Arrange
            SendDirectMessageRequest request = SendDirectMessageRequest.builder()
                    .receiverId(receiverId)
                    .content("Hello!")
                    .build();

            when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
            when(userRepository.findById(receiverId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.sendMessage(request, senderId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(directMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when sending message to yourself")
        void shouldThrowWhenSendingToSelf() {
            // Arrange
            SendDirectMessageRequest request = SendDirectMessageRequest.builder()
                    .receiverId(senderId)
                    .content("Hello me!")
                    .build();

            when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.sendMessage(request, senderId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot send a message to yourself");

            verify(directMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should set read to false and deleted to false on new message")
        void shouldSetDefaultValuesOnNewMessage() {
            // Arrange
            SendDirectMessageRequest request = SendDirectMessageRequest.builder()
                    .receiverId(receiverId)
                    .content("New message")
                    .build();

            DirectMessage saved = buildMessage(UUID.randomUUID(), senderId, receiverId, "New message", false, false);

            when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
            when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
            when(directMessageRepository.save(any(DirectMessage.class))).thenReturn(saved);

            // Act
            directMessageService.sendMessage(request, senderId);

            // Assert
            ArgumentCaptor<DirectMessage> captor = ArgumentCaptor.forClass(DirectMessage.class);
            verify(directMessageRepository).save(captor.capture());
            DirectMessage captured = captor.getValue();
            assertThat(captured.isRead()).isFalse();
            assertThat(captured.isDeleted()).isFalse();
        }
    }

    // ==================== getConversation ====================

    @Nested
    @DisplayName("getConversation")
    class GetConversation {

        @Test
        @DisplayName("should return paginated conversation")
        void shouldReturnPaginatedConversation() {
            // Arrange
            DirectMessage msg1 = buildMessage(UUID.randomUUID(), senderId, receiverId, "Hi", false, false);
            DirectMessage msg2 = buildMessage(UUID.randomUUID(), receiverId, senderId, "Hello", true, false);
            Page<DirectMessage> page = new PageImpl<>(List.of(msg1, msg2));

            when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
            when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
            when(directMessageRepository.findConversation(eq(senderId), eq(receiverId), any(Pageable.class)))
                    .thenReturn(page);

            // Act
            Page<DirectMessageResponse> result = directMessageService.getConversation(senderId, receiverId, 0, 50);

            // Assert
            assertThat(result.getContent()).hasSize(2);
            verify(directMessageRepository).findConversation(eq(senderId), eq(receiverId), any(Pageable.class));
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            // Arrange
            when(userRepository.findById(senderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.getConversation(senderId, receiverId, 0, 50))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("should cap page size to maximum")
        void shouldCapPageSizeToMax() {
            // Arrange
            when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
            when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
            when(directMessageRepository.findConversation(any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // Act
            directMessageService.getConversation(senderId, receiverId, 0, 500);

            // Assert
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(directMessageRepository).findConversation(any(), any(), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        }
    }

    // ==================== markAsRead ====================

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("should mark message as read for receiver")
        void shouldMarkAsReadForReceiver() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            DirectMessage message = buildMessage(messageId, senderId, receiverId, "Hello", false, false);

            when(directMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(directMessageRepository.save(any(DirectMessage.class))).thenReturn(message);

            // Act
            directMessageService.markAsRead(messageId, receiverId);

            // Assert
            ArgumentCaptor<DirectMessage> captor = ArgumentCaptor.forClass(DirectMessage.class);
            verify(directMessageRepository).save(captor.capture());
            assertThat(captor.getValue().isRead()).isTrue();
        }

        @Test
        @DisplayName("should throw when non-receiver tries to mark as read")
        void shouldThrowWhenNonReceiverMarksAsRead() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            DirectMessage message = buildMessage(messageId, senderId, receiverId, "Hello", false, false);

            when(directMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.markAsRead(messageId, senderId))
                    .isInstanceOf(AccessDeniedException.class);

            verify(directMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not save if already read")
        void shouldNotSaveIfAlreadyRead() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            DirectMessage message = buildMessage(messageId, senderId, receiverId, "Hello", true, false);

            when(directMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            // Act
            directMessageService.markAsRead(messageId, receiverId);

            // Assert
            verify(directMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when message not found")
        void shouldThrowWhenMessageNotFound() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            when(directMessageRepository.findById(messageId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.markAsRead(messageId, receiverId))
                    .isInstanceOf(DirectMessageNotFoundException.class);
        }
    }

    // ==================== editMessage ====================

    @Nested
    @DisplayName("editMessage")
    class EditMessage {

        @Test
        @DisplayName("should edit message successfully")
        void shouldEditMessageSuccessfully() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            DirectMessage message = buildMessage(messageId, senderId, receiverId, "Original", false, false);
            UpdateDirectMessageRequest request = UpdateDirectMessageRequest.builder()
                    .content("Updated")
                    .build();

            when(directMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(directMessageRepository.save(any(DirectMessage.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
            when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));

            // Act
            DirectMessageResponse response = directMessageService.editMessage(messageId, request, senderId);

            // Assert
            assertThat(response.getContent()).isEqualTo("Updated");
            assertThat(response.isEdited()).isTrue();
        }

        @Test
        @DisplayName("should throw when non-author tries to edit")
        void shouldThrowWhenNonAuthorEdits() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            DirectMessage message = buildMessage(messageId, senderId, receiverId, "Original", false, false);
            UpdateDirectMessageRequest request = UpdateDirectMessageRequest.builder()
                    .content("Hacked!")
                    .build();

            when(directMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.editMessage(messageId, request, receiverId))
                    .isInstanceOf(AccessDeniedException.class);

            verify(directMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when editing a deleted message")
        void shouldThrowWhenEditingDeletedMessage() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            DirectMessage message = buildMessage(messageId, senderId, receiverId, "Deleted", false, true);
            UpdateDirectMessageRequest request = UpdateDirectMessageRequest.builder()
                    .content("Edit attempt")
                    .build();

            when(directMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.editMessage(messageId, request, senderId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot edit a deleted message");
        }
    }

    // ==================== deleteMessage ====================

    @Nested
    @DisplayName("deleteMessage")
    class DeleteMessage {

        @Test
        @DisplayName("should soft delete message")
        void shouldSoftDeleteMessage() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            DirectMessage message = buildMessage(messageId, senderId, receiverId, "To delete", false, false);

            when(directMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(directMessageRepository.save(any(DirectMessage.class))).thenReturn(message);

            // Act
            directMessageService.deleteMessage(messageId, senderId);

            // Assert
            ArgumentCaptor<DirectMessage> captor = ArgumentCaptor.forClass(DirectMessage.class);
            verify(directMessageRepository).save(captor.capture());
            DirectMessage captured = captor.getValue();
            assertThat(captured.isDeleted()).isTrue();
            assertThat(captured.getContent()).isEqualTo("Ce message a ete supprime.");
        }

        @Test
        @DisplayName("should throw when non-author tries to delete")
        void shouldThrowWhenNonAuthorDeletes() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            DirectMessage message = buildMessage(messageId, senderId, receiverId, "My message", false, false);

            when(directMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.deleteMessage(messageId, receiverId))
                    .isInstanceOf(AccessDeniedException.class);

            verify(directMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when message not found")
        void shouldThrowWhenMessageNotFoundForDelete() {
            // Arrange
            UUID messageId = UUID.randomUUID();
            when(directMessageRepository.findById(messageId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.deleteMessage(messageId, senderId))
                    .isInstanceOf(DirectMessageNotFoundException.class);
        }
    }

    // ==================== getConversations ====================

    @Nested
    @DisplayName("getConversations")
    class GetConversations {

        @Test
        @DisplayName("should return list of conversations sorted by latest message")
        void shouldReturnConversationsSortedByLatest() {
            // Arrange
            UUID otherUser1Id = UUID.randomUUID();
            UUID otherUser2Id = UUID.randomUUID();

            User otherUser1 = User.builder()
                    .id(otherUser1Id).firstName("Charlie").lastName("One").build();
            User otherUser2 = User.builder()
                    .id(otherUser2Id).firstName("Diana").lastName("Two").build();

            DirectMessage msg1 = DirectMessage.builder()
                    .id(UUID.randomUUID())
                    .senderId(senderId)
                    .receiverId(otherUser1Id)
                    .content("Msg to Charlie")
                    .read(false)
                    .deleted(false)
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .build();

            DirectMessage msg2 = DirectMessage.builder()
                    .id(UUID.randomUUID())
                    .senderId(otherUser2Id)
                    .receiverId(senderId)
                    .content("Msg from Diana")
                    .read(false)
                    .deleted(false)
                    .createdAt(LocalDateTime.now().minusMinutes(5))
                    .build();

            when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
            when(userRepository.findById(otherUser1Id)).thenReturn(Optional.of(otherUser1));
            when(userRepository.findById(otherUser2Id)).thenReturn(Optional.of(otherUser2));
            when(directMessageRepository.findLatestMessagePerConversation(senderId))
                    .thenReturn(List.of(msg1, msg2));
            when(directMessageRepository.countUnreadFromSender(senderId, otherUser1Id)).thenReturn(0);
            when(directMessageRepository.countUnreadFromSender(senderId, otherUser2Id)).thenReturn(3);

            // Act
            List<ConversationResponse> conversations = directMessageService.getConversations(senderId);

            // Assert
            assertThat(conversations).hasSize(2);
            // Most recent first (Diana's message is more recent)
            assertThat(conversations.get(0).getUserName()).isEqualTo("Diana Two");
            assertThat(conversations.get(0).getUnreadCount()).isEqualTo(3);
            assertThat(conversations.get(1).getUserName()).isEqualTo("Charlie One");
            assertThat(conversations.get(1).getUnreadCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return empty list when no conversations")
        void shouldReturnEmptyListWhenNoConversations() {
            // Arrange
            when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
            when(directMessageRepository.findLatestMessagePerConversation(senderId))
                    .thenReturn(Collections.emptyList());

            // Act
            List<ConversationResponse> conversations = directMessageService.getConversations(senderId);

            // Assert
            assertThat(conversations).isEmpty();
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFoundForConversations() {
            // Arrange
            when(userRepository.findById(senderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> directMessageService.getConversations(senderId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ==================== getUnreadCount ====================

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("should return unread count")
        void shouldReturnUnreadCount() {
            // Arrange
            when(directMessageRepository.countUnreadByReceiverId(senderId)).thenReturn(7);

            // Act
            UnreadCountResponse response = directMessageService.getUnreadCount(senderId);

            // Assert
            assertThat(response.getUnreadCount()).isEqualTo(7);
        }

        @Test
        @DisplayName("should return zero when no unread messages")
        void shouldReturnZeroWhenNoUnread() {
            // Arrange
            when(directMessageRepository.countUnreadByReceiverId(senderId)).thenReturn(0);

            // Act
            UnreadCountResponse response = directMessageService.getUnreadCount(senderId);

            // Assert
            assertThat(response.getUnreadCount()).isEqualTo(0);
        }
    }

    // ==================== markConversationAsRead ====================

    @Nested
    @DisplayName("markConversationAsRead")
    class MarkConversationAsRead {

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            // Act
            directMessageService.markConversationAsRead(receiverId, senderId);

            // Assert
            verify(directMessageRepository).markConversationAsRead(receiverId, senderId);
        }
    }
}
