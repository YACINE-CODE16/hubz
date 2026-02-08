package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateEventRequest;
import com.hubz.application.dto.request.InviteEventParticipantRequest;
import com.hubz.application.dto.request.RespondEventInvitationRequest;
import com.hubz.application.dto.request.UpdateEventRequest;
import com.hubz.application.dto.response.EventParticipantResponse;
import com.hubz.application.dto.response.EventResponse;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.EventService;
import com.hubz.application.service.ICalService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Events", description = "Calendar events and scheduling")
public class EventController {

    private final EventService eventService;
    private final ICalService iCalService;
    private final UserRepositoryPort userRepositoryPort;

    // ==================== Event CRUD ====================

    @Operation(
            summary = "Get organization events",
            description = """
                    Returns events for a specific organization. Optionally filter by time range.

                    When `start` and `end` are provided:
                    - If `expandRecurring=true` (default), recurring events are expanded to show all occurrences in the range
                    - If `expandRecurring=false`, only the parent event is returned
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @GetMapping("/api/organizations/{orgId}/events")
    public ResponseEntity<List<EventResponse>> getByOrganization(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Parameter(description = "Start of time range (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End of time range (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @Parameter(description = "Expand recurring events to individual occurrences") @RequestParam(required = false, defaultValue = "true") boolean expandRecurring,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);

        if (start != null && end != null) {
            if (expandRecurring) {
                return ResponseEntity.ok(
                        eventService.getByOrganizationWithRecurrence(orgId, start, end, currentUserId)
                );
            }
            return ResponseEntity.ok(
                    eventService.getByOrganizationAndTimeRange(orgId, start, end, currentUserId)
            );
        }

        return ResponseEntity.ok(eventService.getByOrganization(orgId, currentUserId));
    }

    @Operation(
            summary = "Create organization event",
            description = "Creates a new event in the specified organization. Supports recurring events."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event created successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @PostMapping("/api/organizations/{orgId}/events")
    public ResponseEntity<EventResponse> createOrganizationEvent(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(request, orgId, userId));
    }

    @Operation(
            summary = "Get personal events",
            description = "Returns personal events for the current user (not associated with any organization)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/api/users/me/events")
    public ResponseEntity<List<EventResponse>> getPersonalEvents(
            @Parameter(description = "Start of time range (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End of time range (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @Parameter(description = "Expand recurring events to individual occurrences") @RequestParam(required = false, defaultValue = "true") boolean expandRecurring,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);

        if (start != null && end != null) {
            if (expandRecurring) {
                return ResponseEntity.ok(
                        eventService.getPersonalEventsWithRecurrence(userId, start, end)
                );
            }
            return ResponseEntity.ok(
                    eventService.getPersonalEventsByTimeRange(userId, start, end)
            );
        }

        return ResponseEntity.ok(eventService.getPersonalEvents(userId));
    }

    @Operation(
            summary = "Create personal event",
            description = "Creates a new personal event for the current user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event created successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/api/users/me/events")
    public ResponseEntity<EventResponse> createPersonalEvent(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(request, null, userId));
    }

    @Operation(
            summary = "Get event by ID",
            description = "Returns a specific event with its details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/api/events/{id}")
    public ResponseEntity<EventResponse> getById(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(eventService.getById(id, currentUserId));
    }

    @Operation(
            summary = "Update event",
            description = "Updates an event's details. User must be the creator or have appropriate permissions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event updated successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PutMapping("/api/events/{id}")
    public ResponseEntity<EventResponse> update(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(eventService.update(id, request, currentUserId));
    }

    @Operation(
            summary = "Delete event",
            description = """
                    Deletes an event. For recurring events:
                    - If `deleteAllOccurrences=false`, only this occurrence is deleted
                    - If `deleteAllOccurrences=true`, the parent event and all occurrences are deleted
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @DeleteMapping("/api/events/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Delete all occurrences for recurring events") @RequestParam(required = false, defaultValue = "false") boolean deleteAllOccurrences,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        eventService.delete(id, currentUserId, deleteAllOccurrences);
        return ResponseEntity.noContent().build();
    }

    // ==================== Recurring Events ====================

    @Operation(
            summary = "Get recurring event occurrences",
            description = "Returns all occurrences of a recurring event within a specified time range."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Occurrences retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Event is not recurring or invalid time range"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/api/events/{id}/occurrences")
    public ResponseEntity<List<EventResponse>> getOccurrences(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Start of time range (required, ISO format)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End of time range (required, ISO format)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(eventService.getRecurringEventOccurrences(id, start, end, currentUserId));
    }

    // ==================== Participant Management ====================

    @Operation(
            summary = "Get event participants",
            description = "Returns all participants of an event with their status (INVITED, ACCEPTED, DECLINED)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participants retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventParticipantResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/api/events/{eventId}/participants")
    public ResponseEntity<List<EventParticipantResponse>> getParticipants(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(eventService.getParticipants(eventId, currentUserId));
    }

    @Operation(
            summary = "Invite participant",
            description = "Invites a user to an event. Only the event creator or organization admin can invite participants."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Participant invited successfully",
                    content = @Content(schema = @Schema(implementation = EventParticipantResponse.class))),
            @ApiResponse(responseCode = "400", description = "User already invited"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Event or user not found")
    })
    @PostMapping("/api/events/{eventId}/participants")
    public ResponseEntity<EventParticipantResponse> inviteParticipant(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @Valid @RequestBody InviteEventParticipantRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.inviteParticipant(eventId, request, currentUserId));
    }

    @Operation(
            summary = "Respond to invitation",
            description = "Accepts or declines an event invitation. Only the invited user can respond."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Response recorded successfully",
                    content = @Content(schema = @Schema(implementation = EventParticipantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid response"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Invitation not found")
    })
    @PostMapping("/api/events/{eventId}/respond")
    public ResponseEntity<EventParticipantResponse> respondToInvitation(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @Valid @RequestBody RespondEventInvitationRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(eventService.respondToInvitation(eventId, request, currentUserId));
    }

    @Operation(
            summary = "Remove participant",
            description = "Removes a participant from an event. Event creator or participant themselves can remove."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participant removed successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Event or participant not found")
    })
    @DeleteMapping("/api/events/{eventId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @Parameter(description = "Event ID") @PathVariable UUID eventId,
            @Parameter(description = "User ID to remove") @PathVariable UUID userId,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        eventService.removeParticipant(eventId, userId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get event invitations",
            description = "Returns all events where the current user is a participant (invited or accepted)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitations retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/api/users/me/event-invitations")
    public ResponseEntity<List<EventResponse>> getEventsWhereParticipant(
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        return ResponseEntity.ok(eventService.getEventsWhereParticipant(userId));
    }

    // ==================== iCal Export ====================

    @Operation(
            summary = "Export event to iCal",
            description = "Exports a single event in iCalendar (.ics) format for import into calendar applications."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "iCal file generated successfully",
                    content = @Content(mediaType = "text/calendar", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/api/events/{id}/ical")
    public ResponseEntity<String> exportEventToICal(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = resolveUserId(authentication);
        String icalContent = iCalService.exportEventToICal(id, currentUserId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar"));
        headers.setContentDispositionFormData("attachment", "event-" + id + ".ics");

        return ResponseEntity.ok()
                .headers(headers)
                .body(icalContent);
    }

    @Operation(
            summary = "Export personal events to iCal",
            description = "Exports all personal events in iCalendar format."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "iCal file generated successfully",
                    content = @Content(mediaType = "text/calendar", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/api/users/me/events/ical")
    public ResponseEntity<String> exportPersonalEventsToICal(
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        String icalContent = iCalService.exportPersonalEventsToICal(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar"));
        headers.setContentDispositionFormData("attachment", "hubz-personal-events.ics");

        return ResponseEntity.ok()
                .headers(headers)
                .body(icalContent);
    }

    @Operation(
            summary = "Export all events to iCal",
            description = "Exports all events (personal and organization) in iCalendar format."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "iCal file generated successfully",
                    content = @Content(mediaType = "text/calendar", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/api/users/me/all-events/ical")
    public ResponseEntity<String> exportAllEventsToICal(
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        String icalContent = iCalService.exportAllEventsToICal(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar"));
        headers.setContentDispositionFormData("attachment", "hubz-all-events.ics");

        return ResponseEntity.ok()
                .headers(headers)
                .body(icalContent);
    }

    // ==================== Helper Methods ====================

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
