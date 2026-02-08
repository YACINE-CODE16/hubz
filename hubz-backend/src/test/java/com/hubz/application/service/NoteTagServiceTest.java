package com.hubz.application.service;

import com.hubz.application.dto.request.CreateNoteTagRequest;
import com.hubz.application.dto.request.UpdateNoteTagRequest;
import com.hubz.application.dto.response.NoteTagResponse;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.application.port.out.NoteTagRepositoryPort;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.exception.NoteTagNotFoundException;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.NoteTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteTagServiceTest {

    @Mock
    private NoteTagRepositoryPort noteTagRepository;

    @Mock
    private NoteRepositoryPort noteRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private NoteTagService noteTagService;

    private UUID organizationId;
    private UUID userId;
    private UUID tagId;
    private UUID noteId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tagId = UUID.randomUUID();
        noteId = UUID.randomUUID();
    }

    @Test
    void shouldCreateTag() {
        // Given
        CreateNoteTagRequest request = CreateNoteTagRequest.builder()
                .name("Important")
                .color("#FF5733")
                .build();

        NoteTag savedTag = NoteTag.builder()
                .id(tagId)
                .name("Important")
                .color("#FF5733")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        when(noteTagRepository.save(any(NoteTag.class))).thenReturn(savedTag);

        // When
        NoteTagResponse response = noteTagService.create(request, organizationId, userId);

        // Then
        assertThat(response.getName()).isEqualTo("Important");
        assertThat(response.getColor()).isEqualTo("#FF5733");
        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
        verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        verify(noteTagRepository).save(any(NoteTag.class));
    }

    @Test
    void shouldGetTagById() {
        // Given
        NoteTag tag = NoteTag.builder()
                .id(tagId)
                .name("Important")
                .color("#FF5733")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        when(noteTagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        // When
        NoteTagResponse response = noteTagService.getById(tagId, userId);

        // Then
        assertThat(response.getId()).isEqualTo(tagId);
        assertThat(response.getName()).isEqualTo("Important");
        verify(authorizationService).checkOrganizationAccess(organizationId, userId);
    }

    @Test
    void shouldThrowExceptionWhenTagNotFound() {
        // Given
        when(noteTagRepository.findById(tagId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteTagService.getById(tagId, userId))
                .isInstanceOf(NoteTagNotFoundException.class);
    }

    @Test
    void shouldGetTagsByOrganization() {
        // Given
        NoteTag tag1 = NoteTag.builder()
                .id(UUID.randomUUID())
                .name("Important")
                .color("#FF5733")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        NoteTag tag2 = NoteTag.builder()
                .id(UUID.randomUUID())
                .name("Urgent")
                .color("#FF0000")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        when(noteTagRepository.findByOrganizationId(organizationId)).thenReturn(List.of(tag1, tag2));

        // When
        List<NoteTagResponse> response = noteTagService.getByOrganization(organizationId, userId);

        // Then
        assertThat(response).hasSize(2);
        assertThat(response).extracting(NoteTagResponse::getName)
                .containsExactlyInAnyOrder("Important", "Urgent");
    }

    @Test
    void shouldUpdateTag() {
        // Given
        UpdateNoteTagRequest request = UpdateNoteTagRequest.builder()
                .name("Updated Tag")
                .color("#00FF00")
                .build();

        NoteTag existingTag = NoteTag.builder()
                .id(tagId)
                .name("Original Tag")
                .color("#FF5733")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        NoteTag updatedTag = NoteTag.builder()
                .id(tagId)
                .name("Updated Tag")
                .color("#00FF00")
                .organizationId(organizationId)
                .createdAt(existingTag.getCreatedAt())
                .build();

        when(noteTagRepository.findById(tagId)).thenReturn(Optional.of(existingTag));
        when(noteTagRepository.save(any(NoteTag.class))).thenReturn(updatedTag);

        // When
        NoteTagResponse response = noteTagService.update(tagId, request, userId);

        // Then
        assertThat(response.getName()).isEqualTo("Updated Tag");
        assertThat(response.getColor()).isEqualTo("#00FF00");
        verify(noteTagRepository).save(any(NoteTag.class));
    }

    @Test
    void shouldDeleteTag() {
        // Given
        NoteTag tag = NoteTag.builder()
                .id(tagId)
                .name("Important")
                .color("#FF5733")
                .organizationId(organizationId)
                .build();

        when(noteTagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        // When
        noteTagService.delete(tagId, userId);

        // Then
        verify(noteTagRepository).deleteById(tagId);
    }

    @Test
    void shouldAddTagToNote() {
        // Given
        Note note = Note.builder()
                .id(noteId)
                .title("Test Note")
                .organizationId(organizationId)
                .build();

        NoteTag tag = NoteTag.builder()
                .id(tagId)
                .name("Important")
                .organizationId(organizationId)
                .build();

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(noteTagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        // When
        noteTagService.addTagToNote(noteId, tagId, userId);

        // Then
        verify(noteTagRepository).addTagToNote(noteId, tagId);
    }

    @Test
    void shouldNotAddTagToNoteFromDifferentOrganization() {
        // Given
        UUID otherOrgId = UUID.randomUUID();
        Note note = Note.builder()
                .id(noteId)
                .title("Test Note")
                .organizationId(organizationId)
                .build();

        NoteTag tag = NoteTag.builder()
                .id(tagId)
                .name("Important")
                .organizationId(otherOrgId) // Different organization
                .build();

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(noteTagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        // When & Then
        assertThatThrownBy(() -> noteTagService.addTagToNote(noteId, tagId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag and note must belong to the same organization");
    }

    @Test
    void shouldThrowExceptionWhenNoteNotFoundForAddTag() {
        // Given
        when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteTagService.addTagToNote(noteId, tagId, userId))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void shouldRemoveTagFromNote() {
        // Given
        Note note = Note.builder()
                .id(noteId)
                .title("Test Note")
                .organizationId(organizationId)
                .build();

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

        // When
        noteTagService.removeTagFromNote(noteId, tagId, userId);

        // Then
        verify(noteTagRepository).removeTagFromNote(noteId, tagId);
    }

    @Test
    void shouldSetNoteTags() {
        // Given
        UUID tag1Id = UUID.randomUUID();
        UUID tag2Id = UUID.randomUUID();
        Set<UUID> tagIds = Set.of(tag1Id, tag2Id);

        Note note = Note.builder()
                .id(noteId)
                .title("Test Note")
                .organizationId(organizationId)
                .build();

        NoteTag tag1 = NoteTag.builder()
                .id(tag1Id)
                .name("Tag 1")
                .organizationId(organizationId)
                .build();

        NoteTag tag2 = NoteTag.builder()
                .id(tag2Id)
                .name("Tag 2")
                .organizationId(organizationId)
                .build();

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(noteTagRepository.findByIds(tagIds)).thenReturn(List.of(tag1, tag2));

        // When
        noteTagService.setNoteTags(noteId, tagIds, userId);

        // Then
        verify(noteTagRepository).removeAllTagsFromNote(noteId);
        verify(noteTagRepository).addTagToNote(noteId, tag1Id);
        verify(noteTagRepository).addTagToNote(noteId, tag2Id);
    }

    @Test
    void shouldNotSetNoteTagsFromDifferentOrganization() {
        // Given
        UUID otherOrgId = UUID.randomUUID();
        UUID tagInOtherOrgId = UUID.randomUUID();
        Set<UUID> tagIds = Set.of(tagInOtherOrgId);

        Note note = Note.builder()
                .id(noteId)
                .title("Test Note")
                .organizationId(organizationId)
                .build();

        NoteTag tagInOtherOrg = NoteTag.builder()
                .id(tagInOtherOrgId)
                .name("Tag")
                .organizationId(otherOrgId)
                .build();

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(noteTagRepository.findByIds(tagIds)).thenReturn(List.of(tagInOtherOrg));

        // When & Then
        assertThatThrownBy(() -> noteTagService.setNoteTags(noteId, tagIds, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("All tags must belong to the same organization as the note");
    }

    @Test
    void shouldGetTagsByNote() {
        // Given
        Note note = Note.builder()
                .id(noteId)
                .title("Test Note")
                .organizationId(organizationId)
                .build();

        NoteTag tag1 = NoteTag.builder()
                .id(UUID.randomUUID())
                .name("Important")
                .color("#FF5733")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        NoteTag tag2 = NoteTag.builder()
                .id(UUID.randomUUID())
                .name("Urgent")
                .color("#FF0000")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(noteTagRepository.findTagsByNoteId(noteId)).thenReturn(List.of(tag1, tag2));

        // When
        List<NoteTagResponse> response = noteTagService.getTagsByNote(noteId, userId);

        // Then
        assertThat(response).hasSize(2);
        assertThat(response).extracting(NoteTagResponse::getName)
                .containsExactlyInAnyOrder("Important", "Urgent");
    }
}
