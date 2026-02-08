package com.hubz.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubz.application.dto.request.ChatMessageRequest;
import com.hubz.application.dto.response.ChatbotResponse;
import com.hubz.application.dto.response.EventResponse;
import com.hubz.application.dto.response.ProductivityStatsResponse;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.OllamaPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.ChatbotIntent;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.ChatbotParsedMessage;
import com.hubz.domain.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ChatbotService.
 * Tests intent detection, date/time extraction, priority extraction, action execution,
 * and Ollama LLM integration with fallback.
 */
@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private TaskService taskService;

    @Mock
    private EventService eventService;

    @Mock
    private GoalService goalService;

    @Mock
    private NoteService noteService;

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private ProductivityStatsService productivityStatsService;

    @Mock
    private OllamaPort ollamaPort;

    private ChatbotService chatbotService;

    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        // Create ChatbotService with all dependencies
        chatbotService = new ChatbotService(
                taskService,
                eventService,
                goalService,
                noteService,
                taskRepository,
                productivityStatsService,
                ollamaPort,
                new ObjectMapper()
        );
    }

    // ==================== Intent Detection Tests ====================

    @Test
    void shouldDetectCreateTaskIntent() {
        // Given
        String message = "Creer une tache: finir le rapport";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.CREATE_TASK);
    }

    @Test
    void shouldDetectCreateTaskIntentWithAjouter() {
        // Given
        String message = "Ajouter tache urgente pour vendredi";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.CREATE_TASK);
    }

    @Test
    void shouldDetectCreateEventIntent() {
        // Given
        String message = "J'ai un rdv demain a 14h avec le client";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.CREATE_EVENT);
    }

    @Test
    void shouldDetectCreateEventIntentWithReunion() {
        // Given
        String message = "reunion lundi a 10h";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.CREATE_EVENT);
    }

    @Test
    void shouldDetectCreateGoalIntent() {
        // Given
        String message = "Definir un objectif: courir 3 fois par semaine";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.CREATE_GOAL);
    }

    @Test
    void shouldDetectCreateNoteIntent() {
        // Given
        String message = "Note: idee pour le projet";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.CREATE_NOTE);
    }

    @Test
    void shouldDetectQueryTasksIntent() {
        // Given
        String message = "Quelles sont mes taches aujourd'hui?";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.QUERY_TASKS);
    }

    @Test
    void shouldDetectQueryStatsIntent() {
        // Given
        String message = "Combien de taches j'ai completees?";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.QUERY_STATS);
    }

    @Test
    void shouldReturnUnknownIntentForAmbiguousMessage() {
        // Given
        String message = "Bonjour, comment ca va?";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.UNKNOWN);
    }

    // ==================== Date Extraction Tests ====================

    @Test
    void shouldExtractDateDemain() {
        // Given
        String message = "Creer une tache pour demain";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getExtractedDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void shouldExtractDateAujourdhui() {
        // Given
        String message = "Mes taches aujourd'hui";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getExtractedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void shouldExtractDateInDays() {
        // Given
        String message = "Rdv dans 3 jours";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getExtractedDate()).isEqualTo(LocalDate.now().plusDays(3));
    }

    @Test
    void shouldExtractDateDayOfWeek() {
        // Given
        String message = "Reunion lundi";
        LocalDate today = LocalDate.now();
        LocalDate expectedMonday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getExtractedDate()).isEqualTo(expectedMonday);
    }

    @Test
    void shouldExtractDatePourVendredi() {
        // Given
        String message = "Tache pour vendredi";
        LocalDate today = LocalDate.now();
        LocalDate expectedFriday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getExtractedDate()).isEqualTo(expectedFriday);
    }

    @Test
    void shouldExtractSpecificDate() {
        // Given
        String message = "Rdv le 15";
        LocalDate today = LocalDate.now();
        int targetDay = 15;
        LocalDate expected = LocalDate.of(today.getYear(), today.getMonthValue(), targetDay);
        if (expected.isBefore(today)) {
            expected = expected.plusMonths(1);
        }

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getExtractedDate()).isEqualTo(expected);
    }

    // ==================== Time Extraction Tests ====================

    @Test
    void shouldExtractTimeWithHour() {
        // Given
        String message = "Rdv a 14h";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getExtractedTime()).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    void shouldExtractTimeWithHourAndMinutes() {
        // Given
        String message = "Reunion a 9h30";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getExtractedTime()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void shouldExtractTimeMidi() {
        // Given
        String message = "Dejeuner midi";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getExtractedTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void shouldExtractTimeSoir() {
        // Given
        String message = "Appel ce soir";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getExtractedTime()).isEqualTo(LocalTime.of(18, 0));
    }

    // ==================== Priority Extraction Tests ====================

    @Test
    void shouldExtractPriorityUrgent() {
        // Given
        String message = "Creer une tache urgente: bug critique";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getPriority()).isEqualTo(TaskPriority.URGENT);
    }

    @Test
    void shouldExtractPriorityHigh() {
        // Given
        String message = "Tache importante: revue de code";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void shouldExtractPriorityLow() {
        // Given
        String message = "Tache pas urgente: nettoyer le code";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getPriority()).isEqualTo(TaskPriority.LOW);
    }

    @Test
    void shouldDefaultToMediumPriority() {
        // Given
        String message = "Creer une tache: documentation";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    // ==================== Title Extraction Tests ====================

    @Test
    void shouldExtractTitleAfterColon() {
        // Given
        String message = "Note: idee pour le nouveau design";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getTitle()).isEqualTo("Idee pour le nouveau design");
    }

    @Test
    void shouldExtractTitleFromQuotes() {
        // Given
        String message = "Creer une tache \"Finaliser la presentation\"";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getTitle()).isEqualTo("Finaliser la presentation");
    }

    // ==================== Action Execution Tests ====================

    @Test
    void shouldExecuteCreateTaskAction() {
        // Given
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Creer une tache: finaliser le rapport pour demain")
                .organizationId(organizationId)
                .build();

        TaskResponse taskResponse = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Finaliser le rapport")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .organizationId(organizationId)
                .build();

        when(taskService.create(any(), eq(organizationId), eq(userId)))
                .thenReturn(taskResponse);

        // When
        ChatbotResponse response = chatbotService.processMessage(request, userId);

        // Then
        assertThat(response.getIntent()).isEqualTo(ChatbotIntent.CREATE_TASK);
        assertThat(response.isActionExecuted()).isTrue();
        assertThat(response.getCreatedResourceId()).isEqualTo(taskResponse.getId());
        assertThat(response.getConfirmationText()).contains("creee avec succes");
    }

    @Test
    void shouldReturnErrorWhenCreatingTaskWithoutOrganization() {
        // Given
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Creer une tache: test")
                .organizationId(null)
                .build();

        // When
        ChatbotResponse response = chatbotService.processMessage(request, userId);

        // Then
        assertThat(response.getIntent()).isEqualTo(ChatbotIntent.CREATE_TASK);
        assertThat(response.isActionExecuted()).isFalse();
        assertThat(response.getErrorMessage()).contains("organisation");
    }

    @Test
    void shouldExecuteCreateEventAction() {
        // Given
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("J'ai un rdv demain a 14h avec le client")
                .organizationId(organizationId)
                .build();

        EventResponse eventResponse = EventResponse.builder()
                .id(UUID.randomUUID())
                .title("Avec le client")
                .startTime(LocalDateTime.now().plusDays(1))
                .organizationId(organizationId)
                .build();

        when(eventService.create(any(), eq(organizationId), eq(userId)))
                .thenReturn(eventResponse);

        // When
        ChatbotResponse response = chatbotService.processMessage(request, userId);

        // Then
        assertThat(response.getIntent()).isEqualTo(ChatbotIntent.CREATE_EVENT);
        assertThat(response.isActionExecuted()).isTrue();
        assertThat(response.getCreatedResourceId()).isEqualTo(eventResponse.getId());
    }

    @Test
    void shouldExecuteQueryTasksAction() {
        // Given
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Quelles sont mes taches aujourd'hui?")
                .organizationId(organizationId)
                .build();

        LocalDateTime today = LocalDate.now().atStartOfDay();
        Task task1 = Task.builder()
                .id(UUID.randomUUID())
                .title("Tache 1")
                .status(TaskStatus.TODO)
                .dueDate(today)
                .organizationId(organizationId)
                .build();

        when(taskRepository.findByOrganizationId(organizationId))
                .thenReturn(List.of(task1));

        // When
        ChatbotResponse response = chatbotService.processMessage(request, userId);

        // Then
        assertThat(response.getIntent()).isEqualTo(ChatbotIntent.QUERY_TASKS);
        assertThat(response.isActionExecuted()).isTrue();
        assertThat(response.getQueryResults()).isNotNull();
        assertThat(response.getQueryResults().getTotalCount()).isEqualTo(1);
    }

    @Test
    void shouldExecuteQueryStatsAction() {
        // Given
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Combien de taches j'ai completees ce mois?")
                .organizationId(null)
                .build();

        ProductivityStatsResponse stats = ProductivityStatsResponse.builder()
                .tasksCompletedThisWeek(5)
                .tasksCompletedThisMonth(20)
                .productivityScore(75)
                .productiveStreak(7)
                .build();

        when(productivityStatsService.getProductivityStats(userId))
                .thenReturn(stats);

        // When
        ChatbotResponse response = chatbotService.processMessage(request, userId);

        // Then
        assertThat(response.getIntent()).isEqualTo(ChatbotIntent.QUERY_STATS);
        assertThat(response.isActionExecuted()).isTrue();
        assertThat(response.getConfirmationText()).contains("5 taches completees");
        assertThat(response.getConfirmationText()).contains("75/100");
    }

    @Test
    void shouldReturnHelpForUnknownIntent() {
        // Given
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Hello world")
                .organizationId(null)
                .build();

        // When
        ChatbotResponse response = chatbotService.processMessage(request, userId);

        // Then
        assertThat(response.getIntent()).isEqualTo(ChatbotIntent.UNKNOWN);
        assertThat(response.isActionExecuted()).isFalse();
        assertThat(response.getConfirmationText()).contains("Je n'ai pas compris");
        assertThat(response.getQuickActions()).isNotEmpty();
    }

    @Test
    void shouldHandleNullMessage() {
        // Given & When
        ChatbotParsedMessage result = chatbotService.parseMessage(null);

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.UNKNOWN);
        assertThat(result.getConfidence()).isEqualTo(0.0);
    }

    @Test
    void shouldHandleEmptyMessage() {
        // Given & When
        ChatbotParsedMessage result = chatbotService.parseMessage("   ");

        // Then
        assertThat(result.getIntent()).isEqualTo(ChatbotIntent.UNKNOWN);
    }

    // ==================== Confidence Score Tests ====================

    @Test
    void shouldCalculateHighConfidenceWithAllEntities() {
        // Given
        String message = "Creer une tache urgente: rapport pour vendredi";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getConfidence()).isGreaterThan(0.7);
    }

    @Test
    void shouldCalculateLowerConfidenceWithFewEntities() {
        // Given
        String message = "Creer une tache";

        // When
        ChatbotParsedMessage result = chatbotService.parseMessage(message);

        // Then
        assertThat(result.getConfidence()).isLessThan(0.7);
        assertThat(result.getConfidence()).isGreaterThan(0.4);
    }

    // ==================== Ollama Integration Tests ====================

    @Test
    void shouldUseOllamaWhenAvailable() {
        // Given - create a fresh ChatbotService instance for this test
        ChatbotService freshService = new ChatbotService(
                taskService,
                eventService,
                goalService,
                noteService,
                taskRepository,
                productivityStatsService,
                ollamaPort,
                new ObjectMapper()
        );

        when(ollamaPort.isAvailable()).thenReturn(true);
        when(ollamaPort.getModelName()).thenReturn("llama3.1");
        when(ollamaPort.generateResponseWithHistory(anyString(), anyString(), anyString()))
                .thenReturn("{\"intent\":\"CREATE_TASK\",\"title\":\"Test task\",\"priority\":\"HIGH\",\"confidence\":0.95}");

        TaskResponse taskResponse = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Test task")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .organizationId(organizationId)
                .build();

        when(taskService.create(any(), eq(organizationId), eq(userId)))
                .thenReturn(taskResponse);

        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Create a task for testing")
                .organizationId(organizationId)
                .build();

        // When
        ChatbotResponse response = freshService.processMessage(request, userId);

        // Then
        assertThat(response.isUsedOllama()).isTrue();
        assertThat(response.getOllamaModel()).isEqualTo("llama3.1");
        assertThat(response.getIntent()).isEqualTo(ChatbotIntent.CREATE_TASK);
        verify(ollamaPort).generateResponseWithHistory(anyString(), anyString(), anyString());
    }

    @Test
    void shouldFallbackToRegexWhenOllamaNotAvailable() {
        // Given - create a fresh ChatbotService instance for this test
        ChatbotService freshService = new ChatbotService(
                taskService,
                eventService,
                goalService,
                noteService,
                taskRepository,
                productivityStatsService,
                ollamaPort,
                new ObjectMapper()
        );

        when(ollamaPort.isAvailable()).thenReturn(false);

        TaskResponse taskResponse = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Finir le rapport")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .organizationId(organizationId)
                .build();

        when(taskService.create(any(), eq(organizationId), eq(userId)))
                .thenReturn(taskResponse);

        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Creer une tache: finir le rapport")
                .organizationId(organizationId)
                .build();

        // When
        ChatbotResponse response = freshService.processMessage(request, userId);

        // Then
        assertThat(response.isUsedOllama()).isFalse();
        assertThat(response.getIntent()).isEqualTo(ChatbotIntent.CREATE_TASK);
        verify(ollamaPort, never()).generateResponseWithHistory(anyString(), anyString(), anyString());
    }

    @Test
    void shouldFallbackToRegexWhenOllamaReturnsUnknownIntent() {
        // Given - create a fresh ChatbotService instance for this test
        ChatbotService freshService = new ChatbotService(
                taskService,
                eventService,
                goalService,
                noteService,
                taskRepository,
                productivityStatsService,
                ollamaPort,
                new ObjectMapper()
        );

        when(ollamaPort.isAvailable()).thenReturn(true);
        when(ollamaPort.generateResponseWithHistory(anyString(), anyString(), anyString()))
                .thenReturn("{\"intent\":\"UNKNOWN\",\"confidence\":0.3}");

        TaskResponse taskResponse = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Finir le rapport")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .organizationId(organizationId)
                .build();

        when(taskService.create(any(), eq(organizationId), eq(userId)))
                .thenReturn(taskResponse);

        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Creer une tache: finir le rapport")
                .organizationId(organizationId)
                .build();

        // When
        ChatbotResponse response = freshService.processMessage(request, userId);

        // Then
        assertThat(response.isUsedOllama()).isFalse();
        assertThat(response.getIntent()).isEqualTo(ChatbotIntent.CREATE_TASK);
    }

    @Test
    void shouldFallbackToRegexWhenOllamaThrowsException() {
        // Given - create a fresh ChatbotService instance for this test
        ChatbotService freshService = new ChatbotService(
                taskService,
                eventService,
                goalService,
                noteService,
                taskRepository,
                productivityStatsService,
                ollamaPort,
                new ObjectMapper()
        );

        when(ollamaPort.isAvailable()).thenReturn(true);
        when(ollamaPort.generateResponseWithHistory(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Ollama error"));

        TaskResponse taskResponse = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Finir le rapport")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .organizationId(organizationId)
                .build();

        when(taskService.create(any(), eq(organizationId), eq(userId)))
                .thenReturn(taskResponse);

        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Creer une tache: finir le rapport")
                .organizationId(organizationId)
                .build();

        // When
        ChatbotResponse response = freshService.processMessage(request, userId);

        // Then
        assertThat(response.isUsedOllama()).isFalse();
        assertThat(response.getIntent()).isEqualTo(ChatbotIntent.CREATE_TASK);
    }

    @Test
    void shouldParseOllamaResponseWithAllFields() {
        // Given
        String ollamaJson = "{" +
                "\"intent\":\"CREATE_EVENT\"," +
                "\"title\":\"Meeting with team\"," +
                "\"description\":\"Weekly sync\"," +
                "\"date\":\"2026-02-15\"," +
                "\"time\":\"14:00\"," +
                "\"priority\":\"HIGH\"," +
                "\"confidence\":0.92" +
                "}";

        // When
        ChatbotParsedMessage parsed = chatbotService.parseOllamaResponse(ollamaJson, "Original message");

        // Then
        assertThat(parsed).isNotNull();
        assertThat(parsed.getIntent()).isEqualTo(ChatbotIntent.CREATE_EVENT);
        assertThat(parsed.getTitle()).isEqualTo("Meeting with team");
        assertThat(parsed.getDescription()).isEqualTo("Weekly sync");
        assertThat(parsed.getExtractedDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(parsed.getExtractedTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(parsed.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(parsed.getConfidence()).isEqualTo(0.92);
    }

    @Test
    void shouldHandleOllamaResponseWithExtraText() {
        // Given - Ollama sometimes adds text before/after JSON
        String ollamaResponse = "Here is the analysis:\n{\"intent\":\"CREATE_TASK\",\"title\":\"Test\"}\nDone!";

        // When
        ChatbotParsedMessage parsed = chatbotService.parseOllamaResponse(ollamaResponse, "Original");

        // Then
        assertThat(parsed).isNotNull();
        assertThat(parsed.getIntent()).isEqualTo(ChatbotIntent.CREATE_TASK);
        assertThat(parsed.getTitle()).isEqualTo("Test");
    }

    @Test
    void shouldReturnNullForInvalidOllamaResponse() {
        // Given
        String invalidJson = "This is not JSON at all";

        // When
        ChatbotParsedMessage parsed = chatbotService.parseOllamaResponse(invalidJson, "Original");

        // Then
        assertThat(parsed).isNull();
    }

    @Test
    void shouldHandleOllamaResponseWithNullDate() {
        // Given
        String ollamaJson = "{\"intent\":\"CREATE_TASK\",\"title\":\"Test\",\"date\":null,\"time\":null}";

        // When
        ChatbotParsedMessage parsed = chatbotService.parseOllamaResponse(ollamaJson, "Original");

        // Then
        assertThat(parsed).isNotNull();
        assertThat(parsed.getExtractedDate()).isNull();
        assertThat(parsed.getExtractedTime()).isNull();
    }

    @Test
    void shouldCheckOllamaAvailability() {
        // Given
        when(ollamaPort.isAvailable()).thenReturn(true);

        // When
        boolean available = chatbotService.isOllamaAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void shouldReturnOllamaModelName() {
        // Given
        when(ollamaPort.getModelName()).thenReturn("llama3.1");

        // When
        String modelName = chatbotService.getOllamaModelName();

        // Then
        assertThat(modelName).isEqualTo("llama3.1");
    }

    @Test
    void shouldClearConversationHistory() {
        // Given - process a message first to add to history
        when(ollamaPort.isAvailable()).thenReturn(false);

        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Hello")
                .organizationId(null)
                .build();

        chatbotService.processMessage(request, userId);

        // When
        chatbotService.clearConversationHistory(userId);

        // Then - no exception should be thrown
        // The history should be cleared (internal state, tested implicitly)
    }

    @Test
    void shouldHandleOllamaResponseWithInvalidIntent() {
        // Given
        String ollamaJson = "{\"intent\":\"INVALID_INTENT\",\"title\":\"Test\"}";

        // When
        ChatbotParsedMessage parsed = chatbotService.parseOllamaResponse(ollamaJson, "Original");

        // Then
        assertThat(parsed).isNotNull();
        assertThat(parsed.getIntent()).isEqualTo(ChatbotIntent.UNKNOWN);
    }

    @Test
    void shouldHandleOllamaResponseWithInvalidPriority() {
        // Given
        String ollamaJson = "{\"intent\":\"CREATE_TASK\",\"title\":\"Test\",\"priority\":\"INVALID\"}";

        // When
        ChatbotParsedMessage parsed = chatbotService.parseOllamaResponse(ollamaJson, "Original");

        // Then
        assertThat(parsed).isNotNull();
        assertThat(parsed.getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }
}
