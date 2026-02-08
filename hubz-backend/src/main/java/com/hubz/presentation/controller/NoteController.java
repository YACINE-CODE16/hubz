package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateNoteRequest;
import com.hubz.application.dto.request.UpdateNoteRequest;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.dto.response.NoteResponse;
import com.hubz.application.dto.response.NoteVersionResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.NoteService;
import com.hubz.application.service.NoteVersionService;
import com.hubz.domain.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Notes", description = "Rich-text notes with versioning")
public class NoteController {

    private final NoteService noteService;
    private final NoteVersionService noteVersionService;
    private final UserRepositoryPort userRepositoryPort;

    @Operation(
            summary = "Get organization notes",
            description = """
                    Returns notes for a specific organization. Supports multiple filtering options:
                    - `category`: Filter by note category
                    - `folderId`: Filter by folder
                    - `rootOnly`: Get only root-level notes (not in any folder)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notes retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NoteResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @GetMapping("/api/organizations/{orgId}/notes")
    public ResponseEntity<List<NoteResponse>> getByOrganization(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Filter by folder ID") @RequestParam(required = false) UUID folderId,
            @Parameter(description = "Get only root-level notes") @RequestParam(required = false, defaultValue = "false") boolean rootOnly,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);

        // Filter by folder
        if (folderId != null) {
            return ResponseEntity.ok(
                    noteService.getByOrganizationAndFolder(orgId, folderId, currentUserId)
            );
        }

        // Get only root-level notes (no folder)
        if (rootOnly) {
            return ResponseEntity.ok(
                    noteService.getByOrganizationAndFolder(orgId, null, currentUserId)
            );
        }

        // Filter by category
        if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(
                    noteService.getByOrganizationAndCategory(orgId, category, currentUserId)
            );
        }

        // Get all notes
        return ResponseEntity.ok(noteService.getByOrganization(orgId, currentUserId));
    }

    @Operation(
            summary = "Get note by ID",
            description = "Returns a specific note with its content and metadata."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Note retrieved successfully",
                    content = @Content(schema = @Schema(implementation = NoteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Note not found")
    })
    @GetMapping("/api/notes/{id}")
    public ResponseEntity<NoteResponse> getById(
            @Parameter(description = "Note ID") @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteService.getById(id, currentUserId));
    }

    @Operation(
            summary = "Create note",
            description = "Creates a new note in the specified organization. Supports rich-text content (Markdown/HTML)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Note created successfully",
                    content = @Content(schema = @Schema(implementation = NoteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @PostMapping("/api/organizations/{orgId}/notes")
    public ResponseEntity<NoteResponse> create(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Valid @RequestBody CreateNoteRequest request,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.create(request, orgId, userId));
    }

    @Operation(
            summary = "Update note",
            description = "Updates a note's content and metadata. Creates a new version when content changes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Note updated successfully",
                    content = @Content(schema = @Schema(implementation = NoteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Note not found")
    })
    @PutMapping("/api/notes/{id}")
    public ResponseEntity<NoteResponse> update(
            @Parameter(description = "Note ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateNoteRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteService.update(id, request, currentUserId));
    }

    @Operation(
            summary = "Move note to folder",
            description = "Moves a note to a different folder. Pass null folderId to move to root level."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Note moved successfully",
                    content = @Content(schema = @Schema(implementation = NoteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Note or folder not found")
    })
    @PatchMapping("/api/notes/{id}/folder")
    public ResponseEntity<NoteResponse> moveToFolder(
            @Parameter(description = "Note ID") @PathVariable UUID id,
            @Parameter(description = "Target folder ID (null for root)") @RequestParam(required = false) UUID folderId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteService.moveToFolder(id, folderId, currentUserId));
    }

    @Operation(
            summary = "Delete note",
            description = "Permanently deletes a note and all its versions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Note deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Note not found")
    })
    @DeleteMapping("/api/notes/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Note ID") @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        noteService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Search notes",
            description = "Full-text search across note titles and content. Case-insensitive matching."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NoteResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @GetMapping("/api/organizations/{orgId}/notes/search")
    public ResponseEntity<List<NoteResponse>> searchNotes(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") String q,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteService.searchNotes(orgId, q, currentUserId));
    }

    // ========== Note Version Endpoints ==========

    @Operation(
            summary = "Get note versions",
            description = "Returns the version history of a note, ordered by version number descending."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versions retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NoteVersionResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Note not found")
    })
    @GetMapping("/api/notes/{id}/versions")
    public ResponseEntity<List<NoteVersionResponse>> getVersions(
            @Parameter(description = "Note ID") @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteVersionService.getVersionsByNoteId(id, currentUserId));
    }

    @Operation(
            summary = "Get specific version",
            description = "Returns a specific version of a note."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Version retrieved successfully",
                    content = @Content(schema = @Schema(implementation = NoteVersionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Note or version not found")
    })
    @GetMapping("/api/notes/{noteId}/versions/{versionId}")
    public ResponseEntity<NoteVersionResponse> getVersion(
            @Parameter(description = "Note ID") @PathVariable UUID noteId,
            @Parameter(description = "Version ID") @PathVariable UUID versionId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteVersionService.getVersionById(versionId, currentUserId));
    }

    @Operation(
            summary = "Restore version",
            description = "Restores a note to a previous version. Creates a new version with the restored content."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Version restored successfully",
                    content = @Content(schema = @Schema(implementation = NoteVersionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Note or version not found")
    })
    @PostMapping("/api/notes/{id}/restore/{versionId}")
    public ResponseEntity<NoteVersionResponse> restoreVersion(
            @Parameter(description = "Note ID") @PathVariable UUID id,
            @Parameter(description = "Version ID to restore") @PathVariable UUID versionId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(noteVersionService.restoreVersion(id, versionId, currentUserId));
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
