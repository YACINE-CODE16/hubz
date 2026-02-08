package com.hubz.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubz.application.dto.request.ChatMessageRequest;
import com.hubz.application.dto.request.CreateEventRequest;
import com.hubz.application.dto.request.CreateGoalRequest;
import com.hubz.application.dto.request.CreateNoteRequest;
import com.hubz.application.dto.request.CreateTaskRequest;
import com.hubz.application.dto.response.ChatbotResponse;
import com.hubz.application.dto.response.EventResponse;
import com.hubz.application.dto.response.GoalResponse;
import com.hubz.application.dto.response.NoteResponse;
import com.hubz.application.dto.response.ProductivityStatsResponse;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.OllamaPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.ChatbotIntent;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.ChatbotParsedMessage;
import com.hubz.domain.model.ConversationHistory;
import com.hubz.domain.model.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing and executing chatbot commands.
 * Uses Ollama LLM for advanced NLP when available, with fallback to regex-based parsing.
 */
@Service
@Slf4j
public class ChatbotService {

    private final TaskService taskService;
    private final EventService eventService;
    private final GoalService goalService;
    private final NoteService noteService;
    private final TaskRepositoryPort taskRepository;
    private final ProductivityStatsService productivityStatsService;
    private final OllamaPort ollamaPort;
    private final ObjectMapper objectMapper;

    // In-memory conversation history cache (per user)
    private final Map<UUID, ConversationHistory> conversationHistoryCache = new ConcurrentHashMap<>();

    // Flag to track if Ollama is available (cached for performance)
    private Boolean ollamaAvailable = null;
    private long ollamaCheckTimestamp = 0;
    private static final long OLLAMA_CHECK_INTERVAL_MS = 60000; // Check every minute

    public ChatbotService(
            TaskService taskService,
            EventService eventService,
            GoalService goalService,
            NoteService noteService,
            TaskRepositoryPort taskRepository,
            ProductivityStatsService productivityStatsService,
            OllamaPort ollamaPort,
            ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.eventService = eventService;
        this.goalService = goalService;
        this.noteService = noteService;
        this.taskRepository = taskRepository;
        this.productivityStatsService = productivityStatsService;
        this.ollamaPort = ollamaPort;
        this.objectMapper = objectMapper;
    }

    // Intent patterns (case-insensitive)
    private static final Pattern CREATE_TASK_PATTERN = Pattern.compile(
            "(?i)(creer|ajouter|nouvelle?|faire)\\s+(une?\\s+)?tache|tache\\s*:", Pattern.UNICODE_CASE);
    private static final Pattern CREATE_EVENT_PATTERN = Pattern.compile(
            "(?i)(creer|ajouter|nouveau?|j'ai)\\s+(un\\s+)?(rdv|rendez-vous|evenement|reunion|meeting)|" +
            "^(rdv|rendez-vous|reunion|meeting)\\s+|" +
            "(rdv|rendez-vous|reunion|meeting)\\s+(le|a|avec)", Pattern.UNICODE_CASE);
    private static final Pattern CREATE_GOAL_PATTERN = Pattern.compile(
            "(?i)(creer|ajouter|nouveau?|definir)\\s+(un\\s+)?objectif|objectif\\s*:", Pattern.UNICODE_CASE);
    private static final Pattern CREATE_NOTE_PATTERN = Pattern.compile(
            "(?i)(creer|ajouter|nouvelle?|ecrire)\\s+(une?\\s+)?note|note\\s*:|idee\\s*:", Pattern.UNICODE_CASE);
    private static final Pattern QUERY_TASKS_PATTERN = Pattern.compile(
            "(?i)(quelles?|combien|liste|affiche|montre|voir)\\s+.*(taches?|tasks?)|" +
            "(mes\\s+)?taches?\\s+(du\\s+jour|aujourd'?hui|cette\\s+semaine)", Pattern.UNICODE_CASE);
    private static final Pattern QUERY_STATS_PATTERN = Pattern.compile(
            "(?i)(statistiques?|stats?|productivite|resume|bilan)|" +
            "(taches?|objectifs?)\\s+.*(complete|termine|fini)", Pattern.UNICODE_CASE);

    // Date patterns
    private static final Pattern DATE_TOMORROW = Pattern.compile("(?i)demain", Pattern.UNICODE_CASE);
    private static final Pattern DATE_TODAY = Pattern.compile("(?i)aujourd'?hui", Pattern.UNICODE_CASE);
    private static final Pattern DATE_NEXT_WEEK = Pattern.compile("(?i)semaine\\s+prochaine", Pattern.UNICODE_CASE);
    private static final Pattern DATE_IN_DAYS = Pattern.compile("(?i)dans\\s+(\\d+)\\s+jours?", Pattern.UNICODE_CASE);
    private static final Pattern DATE_DAY_OF_WEEK = Pattern.compile(
            "(?i)(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)(\\s+prochain)?", Pattern.UNICODE_CASE);
    private static final Pattern DATE_SPECIFIC = Pattern.compile(
            "(?i)le\\s+(\\d{1,2})(?:\\s+(janvier|fevrier|mars|avril|mai|juin|juillet|aout|septembre|octobre|novembre|decembre))?",
            Pattern.UNICODE_CASE);
    private static final Pattern DATE_POUR = Pattern.compile(
            "(?i)pour\\s+(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)", Pattern.UNICODE_CASE);

    // Time patterns
    private static final Pattern TIME_HOUR = Pattern.compile("(?i)a\\s+(\\d{1,2})\\s*h\\s*(\\d{2})?", Pattern.UNICODE_CASE);
    private static final Pattern TIME_MIDI = Pattern.compile("(?i)\\bmidi\\b", Pattern.UNICODE_CASE);
    private static final Pattern TIME_MORNING = Pattern.compile("(?i)\\b(le\\s+)?matin\\b", Pattern.UNICODE_CASE);
    private static final Pattern TIME_AFTERNOON = Pattern.compile("(?i)\\b(cet?\\s+)?apres-?midi\\b", Pattern.UNICODE_CASE);
    private static final Pattern TIME_EVENING = Pattern.compile("(?i)\\b(ce\\s+)?soir\\b", Pattern.UNICODE_CASE);

    // Priority patterns - check LOW first as it contains "urgent" within "pas urgent"
    private static final Pattern PRIORITY_LOW = Pattern.compile(
            "(?i)\\b(pas\\s+urgent|pas\\s+urgente?|basse\\s+priorite|quand\\s+possible)\\b", Pattern.UNICODE_CASE);
    private static final Pattern PRIORITY_URGENT = Pattern.compile(
            "(?i)\\b(urgent|urgente|urgence|asap|immediatement|critique)\\b", Pattern.UNICODE_CASE);
    private static final Pattern PRIORITY_HIGH = Pattern.compile(
            "(?i)\\b(important|importante|prioritaire|haute\\s+priorite)\\b", Pattern.UNICODE_CASE);

    // Title extraction patterns
    private static final Pattern TITLE_COLON = Pattern.compile("[:;]\\s*(.+)$");
    private static final Pattern TITLE_QUOTES = Pattern.compile("[\"'](.+?)[\"']");

    /**
     * Parse a message and extract intent and entities.
     */
    public ChatbotParsedMessage parseMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return ChatbotParsedMessage.builder()
                    .intent(ChatbotIntent.UNKNOWN)
                    .rawMessage(message)
                    .confidence(0.0)
                    .build();
        }

        String normalizedMessage = normalizeMessage(message);

        ChatbotIntent intent = detectIntent(normalizedMessage);
        LocalDate date = extractDate(normalizedMessage);
        LocalTime time = extractTime(normalizedMessage);
        TaskPriority priority = extractPriority(normalizedMessage);
        // Pass original message to extractTitle to preserve case in quotes
        String title = extractTitle(message, intent);

        double confidence = calculateConfidence(intent, date, time, priority, title);

        return ChatbotParsedMessage.builder()
                .intent(intent)
                .title(title)
                .extractedDate(date)
                .extractedTime(time)
                .priority(priority)
                .rawMessage(message)
                .confidence(confidence)
                .build();
    }

    /**
     * Process a chatbot message and execute the corresponding action.
     * Uses Ollama LLM when available, falls back to regex-based parsing.
     */
    @Transactional
    public ChatbotResponse processMessage(ChatMessageRequest request, UUID userId) {
        String message = request.getMessage();
        UUID organizationId = request.getOrganizationId();

        // Try Ollama first if available
        if (isOllamaAvailable()) {
            try {
                ChatbotParsedMessage ollamaParsed = parseMessageWithOllama(message, userId, organizationId);
                if (ollamaParsed != null && ollamaParsed.getIntent() != ChatbotIntent.UNKNOWN) {
                    log.debug("Using Ollama response for intent: {}", ollamaParsed.getIntent());
                    // Update conversation history
                    updateConversationHistory(userId, message, ollamaParsed);
                    return executeAction(ollamaParsed, organizationId, userId, true);
                }
            } catch (Exception e) {
                log.warn("Ollama processing failed, falling back to regex: {}", e.getMessage());
            }
        }

        // Fallback to regex-based parsing
        ChatbotParsedMessage parsed = parseMessage(message);
        return executeAction(parsed, organizationId, userId, false);
    }

    /**
     * Check if Ollama is currently available (with caching).
     */
    public boolean isOllamaAvailable() {
        long now = System.currentTimeMillis();
        if (ollamaAvailable == null || (now - ollamaCheckTimestamp) > OLLAMA_CHECK_INTERVAL_MS) {
            ollamaAvailable = ollamaPort.isAvailable();
            ollamaCheckTimestamp = now;
            log.debug("Ollama availability check: {}", ollamaAvailable);
        }
        return ollamaAvailable;
    }

    /**
     * Get the Ollama model name if available.
     */
    public String getOllamaModelName() {
        return ollamaPort.getModelName();
    }

    /**
     * Execute action based on parsed message.
     */
    private ChatbotResponse executeAction(ChatbotParsedMessage parsed, UUID organizationId, UUID userId, boolean usedOllama) {
        ChatbotResponse response = switch (parsed.getIntent()) {
            case CREATE_TASK -> executeCreateTask(parsed, organizationId, userId);
            case CREATE_EVENT -> executeCreateEvent(parsed, organizationId, userId);
            case CREATE_GOAL -> executeCreateGoal(parsed, organizationId, userId);
            case CREATE_NOTE -> executeCreateNote(parsed, organizationId, userId);
            case QUERY_TASKS -> executeQueryTasks(parsed, organizationId, userId);
            case QUERY_STATS -> executeQueryStats(parsed, userId);
            case UNKNOWN -> buildUnknownResponse(parsed);
        };

        // Add Ollama indicator to response
        if (usedOllama) {
            response.setUsedOllama(true);
            response.setOllamaModel(ollamaPort.getModelName());
        }

        return response;
    }

    /**
     * Parse a message using Ollama LLM for advanced understanding.
     */
    ChatbotParsedMessage parseMessageWithOllama(String message, UUID userId, UUID organizationId) {
        String systemPrompt = buildSystemPrompt(organizationId);
        String conversationContext = getConversationContext(userId);

        String response = ollamaPort.generateResponseWithHistory(message, systemPrompt, conversationContext);

        if (response == null || response.isEmpty()) {
            log.warn("Ollama returned empty response");
            return null;
        }

        return parseOllamaResponse(response, message);
    }

    /**
     * Build system prompt for Ollama with instructions.
     */
    private String buildSystemPrompt(UUID organizationId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un assistant IA pour Hubz, une application de gestion de projets et de productivite.\n");
        prompt.append("Tu dois analyser les messages des utilisateurs et repondre UNIQUEMENT en JSON valide.\n\n");

        prompt.append("INTENTS DISPONIBLES:\n");
        prompt.append("- CREATE_TASK: Creer une nouvelle tache\n");
        prompt.append("- CREATE_EVENT: Creer un evenement ou rendez-vous\n");
        prompt.append("- CREATE_GOAL: Creer un objectif\n");
        prompt.append("- CREATE_NOTE: Creer une note\n");
        prompt.append("- QUERY_TASKS: Lister ou chercher des taches\n");
        prompt.append("- QUERY_STATS: Obtenir des statistiques de productivite\n");
        prompt.append("- UNKNOWN: Si tu ne comprends pas la demande\n\n");

        prompt.append("FORMAT DE REPONSE JSON OBLIGATOIRE:\n");
        prompt.append("{\n");
        prompt.append("  \"intent\": \"CREATE_TASK\",\n");
        prompt.append("  \"title\": \"Titre extrait du message\",\n");
        prompt.append("  \"description\": \"Description optionnelle\",\n");
        prompt.append("  \"date\": \"2026-02-15\",\n");
        prompt.append("  \"time\": \"14:00\",\n");
        prompt.append("  \"priority\": \"HIGH\",\n");
        prompt.append("  \"message\": \"Message de confirmation en francais\",\n");
        prompt.append("  \"confidence\": 0.95\n");
        prompt.append("}\n\n");

        prompt.append("PRIORITES: URGENT, HIGH, MEDIUM, LOW\n");
        prompt.append("DATES: Format ISO (YYYY-MM-DD), ou null si non specifiee\n");
        prompt.append("HEURES: Format HH:MM, ou null si non specifiee\n\n");

        prompt.append("EXEMPLES:\n");
        prompt.append("Message: \"Creer une tache urgente pour finir le rapport demain\"\n");
        prompt.append("Reponse: {\"intent\":\"CREATE_TASK\",\"title\":\"Finir le rapport\",\"priority\":\"URGENT\",\"date\":\"")
                .append(LocalDate.now().plusDays(1)).append("\",\"message\":\"Tache urgente creee pour demain\",\"confidence\":0.95}\n\n");

        prompt.append("Message: \"J'ai un rdv avec Marie lundi a 14h\"\n");
        prompt.append("Reponse: {\"intent\":\"CREATE_EVENT\",\"title\":\"Rendez-vous avec Marie\",\"date\":\"")
                .append(LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)))
                .append("\",\"time\":\"14:00\",\"message\":\"Evenement cree pour lundi a 14h\",\"confidence\":0.92}\n\n");

        if (organizationId != null) {
            prompt.append("CONTEXTE: L'utilisateur est dans une organisation. Les taches et notes seront creees dans cette organisation.\n");
        } else {
            prompt.append("CONTEXTE: L'utilisateur est dans son espace personnel. Les objectifs et evenements seront personnels.\n");
        }

        prompt.append("\nREPONDS UNIQUEMENT EN JSON VALIDE, SANS TEXTE AVANT OU APRES.");

        return prompt.toString();
    }

    /**
     * Get conversation history context for a user.
     */
    private String getConversationContext(UUID userId) {
        ConversationHistory history = conversationHistoryCache.get(userId);
        if (history == null || history.isEmpty()) {
            return "";
        }
        return history.toContextString();
    }

    /**
     * Update conversation history after processing.
     */
    private void updateConversationHistory(UUID userId, String userMessage, ChatbotParsedMessage parsed) {
        ConversationHistory history = conversationHistoryCache.computeIfAbsent(
                userId,
                k -> ConversationHistory.builder().userId(k).build()
        );

        history.addUserMessage(userMessage);

        // Add assistant response summary
        String assistantSummary = String.format(
                "Intent: %s, Title: %s",
                parsed.getIntent(),
                parsed.getTitle() != null ? parsed.getTitle() : "N/A"
        );
        history.addAssistantMessage(assistantSummary);
    }

    /**
     * Parse the JSON response from Ollama into a ChatbotParsedMessage.
     */
    ChatbotParsedMessage parseOllamaResponse(String response, String rawMessage) {
        try {
            // Extract JSON from response (Ollama might add extra text)
            String jsonStr = extractJsonFromResponse(response);
            if (jsonStr == null) {
                log.warn("Could not extract JSON from Ollama response");
                return null;
            }

            JsonNode json = objectMapper.readTree(jsonStr);

            ChatbotIntent intent = parseIntent(json.path("intent").asText("UNKNOWN"));
            String title = json.path("title").asText(null);
            String description = json.path("description").asText(null);
            LocalDate date = parseDate(json.path("date").asText(null));
            LocalTime time = parseTime(json.path("time").asText(null));
            TaskPriority priority = parsePriority(json.path("priority").asText("MEDIUM"));
            double confidence = json.path("confidence").asDouble(0.8);

            return ChatbotParsedMessage.builder()
                    .intent(intent)
                    .title(title)
                    .description(description)
                    .extractedDate(date)
                    .extractedTime(time)
                    .priority(priority)
                    .rawMessage(rawMessage)
                    .confidence(confidence)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Ollama response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract JSON object from Ollama response (which might have extra text).
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        // Find the first { and last }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return null;
    }

    private ChatbotIntent parseIntent(String intentStr) {
        try {
            return ChatbotIntent.valueOf(intentStr.toUpperCase());
        } catch (Exception e) {
            return ChatbotIntent.UNKNOWN;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "null".equals(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty() || "null".equals(timeStr)) {
            return null;
        }
        try {
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            return null;
        }
    }

    private TaskPriority parsePriority(String priorityStr) {
        try {
            return TaskPriority.valueOf(priorityStr.toUpperCase());
        } catch (Exception e) {
            return TaskPriority.MEDIUM;
        }
    }

    /**
     * Clear conversation history for a user.
     */
    public void clearConversationHistory(UUID userId) {
        conversationHistoryCache.remove(userId);
    }

    // ==================== Intent Detection ====================

    ChatbotIntent detectIntent(String message) {
        if (CREATE_TASK_PATTERN.matcher(message).find()) {
            return ChatbotIntent.CREATE_TASK;
        }
        if (CREATE_EVENT_PATTERN.matcher(message).find()) {
            return ChatbotIntent.CREATE_EVENT;
        }
        if (CREATE_GOAL_PATTERN.matcher(message).find()) {
            return ChatbotIntent.CREATE_GOAL;
        }
        if (CREATE_NOTE_PATTERN.matcher(message).find()) {
            return ChatbotIntent.CREATE_NOTE;
        }
        // Check QUERY_STATS before QUERY_TASKS - stats patterns are more specific
        if (QUERY_STATS_PATTERN.matcher(message).find()) {
            return ChatbotIntent.QUERY_STATS;
        }
        if (QUERY_TASKS_PATTERN.matcher(message).find()) {
            return ChatbotIntent.QUERY_TASKS;
        }
        return ChatbotIntent.UNKNOWN;
    }

    // ==================== Date Extraction ====================

    LocalDate extractDate(String message) {
        LocalDate today = LocalDate.now();

        // Check "aujourd'hui"
        if (DATE_TODAY.matcher(message).find()) {
            return today;
        }

        // Check "demain"
        if (DATE_TOMORROW.matcher(message).find()) {
            return today.plusDays(1);
        }

        // Check "semaine prochaine"
        if (DATE_NEXT_WEEK.matcher(message).find()) {
            return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }

        // Check "dans X jours"
        Matcher inDaysMatcher = DATE_IN_DAYS.matcher(message);
        if (inDaysMatcher.find()) {
            int days = Integer.parseInt(inDaysMatcher.group(1));
            return today.plusDays(days);
        }

        // Check "pour lundi/mardi/..."
        Matcher pourDayMatcher = DATE_POUR.matcher(message);
        if (pourDayMatcher.find()) {
            DayOfWeek dayOfWeek = parseDayOfWeek(pourDayMatcher.group(1));
            if (dayOfWeek != null) {
                return today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
            }
        }

        // Check day of week (lundi, mardi, etc.)
        Matcher dayMatcher = DATE_DAY_OF_WEEK.matcher(message);
        if (dayMatcher.find()) {
            DayOfWeek dayOfWeek = parseDayOfWeek(dayMatcher.group(1));
            if (dayOfWeek != null) {
                boolean nextWeek = dayMatcher.group(2) != null;
                if (nextWeek) {
                    return today.with(TemporalAdjusters.next(dayOfWeek));
                }
                return today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
            }
        }

        // Check specific date "le 18" or "le 18 janvier"
        Matcher specificDateMatcher = DATE_SPECIFIC.matcher(message);
        if (specificDateMatcher.find()) {
            int day = Integer.parseInt(specificDateMatcher.group(1));
            String monthStr = specificDateMatcher.group(2);

            int month = today.getMonthValue();
            int year = today.getYear();

            if (monthStr != null) {
                month = parseMonth(monthStr);
            }

            // If the date has already passed this month, assume next month
            LocalDate result = LocalDate.of(year, month, Math.min(day, 28));
            if (result.isBefore(today) && monthStr == null) {
                result = result.plusMonths(1);
            }
            return result;
        }

        return null;
    }

    // ==================== Time Extraction ====================

    LocalTime extractTime(String message) {
        // Check specific time "a 13h" or "a 14h30"
        Matcher timeMatcher = TIME_HOUR.matcher(message);
        if (timeMatcher.find()) {
            int hour = Integer.parseInt(timeMatcher.group(1));
            String minutesStr = timeMatcher.group(2);
            int minutes = minutesStr != null ? Integer.parseInt(minutesStr) : 0;
            if (hour >= 0 && hour <= 23 && minutes >= 0 && minutes <= 59) {
                return LocalTime.of(hour, minutes);
            }
        }

        // Check "midi"
        if (TIME_MIDI.matcher(message).find()) {
            return LocalTime.of(12, 0);
        }

        // Check "matin"
        if (TIME_MORNING.matcher(message).find()) {
            return LocalTime.of(9, 0);
        }

        // Check "apres-midi"
        if (TIME_AFTERNOON.matcher(message).find()) {
            return LocalTime.of(14, 0);
        }

        // Check "soir"
        if (TIME_EVENING.matcher(message).find()) {
            return LocalTime.of(18, 0);
        }

        return null;
    }

    // ==================== Priority Extraction ====================

    TaskPriority extractPriority(String message) {
        // Check LOW first because "pas urgent" contains "urgent"
        if (PRIORITY_LOW.matcher(message).find()) {
            return TaskPriority.LOW;
        }
        if (PRIORITY_URGENT.matcher(message).find()) {
            return TaskPriority.URGENT;
        }
        if (PRIORITY_HIGH.matcher(message).find()) {
            return TaskPriority.HIGH;
        }
        return TaskPriority.MEDIUM;
    }

    // ==================== Title Extraction ====================

    String extractTitle(String message, ChatbotIntent intent) {
        // First, check in the ORIGINAL message (not normalized) for quotes to preserve case
        Matcher quotesMatcher = TITLE_QUOTES.matcher(message);
        if (quotesMatcher.find()) {
            return quotesMatcher.group(1).trim();
        }

        // Try to extract title after colon (use original message for case preservation)
        Matcher colonMatcher = TITLE_COLON.matcher(message);
        if (colonMatcher.find()) {
            return cleanTitle(colonMatcher.group(1));
        }

        // Extract based on intent patterns
        return extractTitleFromIntent(message, intent);
    }

    private String extractTitleFromIntent(String message, ChatbotIntent intent) {
        String cleaned = message;

        // Remove intent keywords
        cleaned = cleaned.replaceAll("(?i)(creer|ajouter|nouvelle?|nouveau?|faire|j'ai|definir|ecrire)\\s+", "");
        cleaned = cleaned.replaceAll("(?i)(une?|un|des|la|le|les)\\s+", "");
        cleaned = cleaned.replaceAll("(?i)(tache|evenement|rdv|rendez-vous|reunion|meeting|objectif|note|idee)\\s*:?\\s*", "");

        // Remove date/time expressions
        cleaned = cleaned.replaceAll("(?i)(pour|le)\\s+(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche).*", "");
        cleaned = cleaned.replaceAll("(?i)demain.*", "");
        cleaned = cleaned.replaceAll("(?i)aujourd'?hui.*", "");
        cleaned = cleaned.replaceAll("(?i)dans\\s+\\d+\\s+jours?.*", "");
        cleaned = cleaned.replaceAll("(?i)a\\s+\\d{1,2}\\s*h\\s*\\d{0,2}.*", "");
        cleaned = cleaned.replaceAll("(?i)le\\s+\\d{1,2}(\\s+\\w+)?.*", "");

        // Remove priority keywords
        cleaned = cleaned.replaceAll("(?i)(urgent|urgente|important|importante|prioritaire)\\s*", "");

        // Clean up
        cleaned = cleanTitle(cleaned);

        return cleaned.isEmpty() ? null : cleaned;
    }

    private String cleanTitle(String title) {
        if (title == null) return null;
        // Remove leading/trailing punctuation and whitespace
        title = title.replaceAll("^[\\s,:;.-]+", "");
        title = title.replaceAll("[\\s,:;.-]+$", "");
        // Capitalize first letter
        if (!title.isEmpty()) {
            title = Character.toUpperCase(title.charAt(0)) + title.substring(1);
        }
        return title.trim();
    }

    // ==================== Action Execution ====================

    private ChatbotResponse executeCreateTask(ChatbotParsedMessage parsed, UUID organizationId, UUID userId) {
        try {
            if (organizationId == null) {
                return buildErrorResponse(parsed, "Veuillez selectionner une organisation pour creer une tache.");
            }

            String title = parsed.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = "Nouvelle tache";
            }

            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle(title);
            request.setDescription(parsed.getDescription());
            request.setPriority(parsed.getPriority() != null ? parsed.getPriority() : TaskPriority.MEDIUM);
            if (parsed.getExtractedDate() != null) {
                // Convert LocalDate to LocalDateTime at midnight
                request.setDueDate(parsed.getExtractedDate().atStartOfDay());
            }

            TaskResponse task = taskService.create(request, organizationId, userId);

            return ChatbotResponse.builder()
                    .intent(ChatbotIntent.CREATE_TASK)
                    .entities(buildEntities(parsed))
                    .confirmationText(buildTaskConfirmation(task))
                    .actionUrl("/org/" + organizationId + "/tasks")
                    .actionExecuted(true)
                    .createdResourceId(task.getId())
                    .quickActions(buildTaskQuickActions(task.getId(), organizationId))
                    .build();

        } catch (Exception e) {
            log.error("Failed to create task from chatbot", e);
            return buildErrorResponse(parsed, "Erreur lors de la creation de la tache: " + e.getMessage());
        }
    }

    private ChatbotResponse executeCreateEvent(ChatbotParsedMessage parsed, UUID organizationId, UUID userId) {
        try {
            String title = parsed.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = "Nouvel evenement";
            }

            LocalDateTime startTime = buildDateTime(parsed.getExtractedDate(), parsed.getExtractedTime());
            if (startTime == null) {
                startTime = LocalDateTime.now().plusHours(1);
            }

            LocalDateTime endTime = startTime.plusHours(1);

            CreateEventRequest request = CreateEventRequest.builder()
                    .title(title)
                    .description(parsed.getDescription())
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();

            EventResponse event;
            if (organizationId != null) {
                event = eventService.create(request, organizationId, userId);
            } else {
                event = eventService.create(request, null, userId);
            }

            String actionUrl = organizationId != null
                    ? "/org/" + organizationId + "/calendar"
                    : "/personal/calendar";

            return ChatbotResponse.builder()
                    .intent(ChatbotIntent.CREATE_EVENT)
                    .entities(buildEntities(parsed))
                    .confirmationText(buildEventConfirmation(event))
                    .actionUrl(actionUrl)
                    .actionExecuted(true)
                    .createdResourceId(event.getId())
                    .quickActions(buildEventQuickActions(event.getId(), organizationId))
                    .build();

        } catch (Exception e) {
            log.error("Failed to create event from chatbot", e);
            return buildErrorResponse(parsed, "Erreur lors de la creation de l'evenement: " + e.getMessage());
        }
    }

    private ChatbotResponse executeCreateGoal(ChatbotParsedMessage parsed, UUID organizationId, UUID userId) {
        try {
            String title = parsed.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = "Nouvel objectif";
            }

            CreateGoalRequest request = CreateGoalRequest.builder()
                    .title(title)
                    .type(GoalType.SHORT)
                    .deadline(parsed.getExtractedDate() != null ? parsed.getExtractedDate() : LocalDate.now().plusWeeks(2))
                    .build();

            // Pass null for organizationId for personal goals
            GoalResponse goal = goalService.create(request, organizationId, userId);

            String actionUrl = organizationId != null
                    ? "/org/" + organizationId + "/goals"
                    : "/personal/goals";

            return ChatbotResponse.builder()
                    .intent(ChatbotIntent.CREATE_GOAL)
                    .entities(buildEntities(parsed))
                    .confirmationText(buildGoalConfirmation(goal))
                    .actionUrl(actionUrl)
                    .actionExecuted(true)
                    .createdResourceId(goal.getId())
                    .quickActions(buildGoalQuickActions(goal.getId(), organizationId))
                    .build();

        } catch (Exception e) {
            log.error("Failed to create goal from chatbot", e);
            return buildErrorResponse(parsed, "Erreur lors de la creation de l'objectif: " + e.getMessage());
        }
    }

    private ChatbotResponse executeCreateNote(ChatbotParsedMessage parsed, UUID organizationId, UUID userId) {
        try {
            if (organizationId == null) {
                return buildErrorResponse(parsed, "Veuillez selectionner une organisation pour creer une note.");
            }

            String title = parsed.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = "Nouvelle note";
            }

            CreateNoteRequest request = CreateNoteRequest.builder()
                    .title(title)
                    .content(parsed.getDescription() != null ? parsed.getDescription() : "")
                    .build();

            NoteResponse note = noteService.create(request, organizationId, userId);

            return ChatbotResponse.builder()
                    .intent(ChatbotIntent.CREATE_NOTE)
                    .entities(buildEntities(parsed))
                    .confirmationText("Note \"" + note.getTitle() + "\" creee avec succes.")
                    .actionUrl("/org/" + organizationId + "/notes")
                    .actionExecuted(true)
                    .createdResourceId(note.getId())
                    .quickActions(buildNoteQuickActions(note.getId(), organizationId))
                    .build();

        } catch (Exception e) {
            log.error("Failed to create note from chatbot", e);
            return buildErrorResponse(parsed, "Erreur lors de la creation de la note: " + e.getMessage());
        }
    }

    private ChatbotResponse executeQueryTasks(ChatbotParsedMessage parsed, UUID organizationId, UUID userId) {
        try {
            List<Task> tasks;
            if (organizationId != null) {
                tasks = taskRepository.findByOrganizationId(organizationId);
            } else {
                tasks = taskRepository.findByAssigneeId(userId);
            }

            // Filter by date if specified
            LocalDate targetDate = parsed.getExtractedDate();
            if (targetDate != null) {
                tasks = tasks.stream()
                        .filter(t -> t.getDueDate() != null && t.getDueDate().toLocalDate().equals(targetDate))
                        .toList();
            }

            // Filter to show only non-completed tasks
            List<Task> activeTasks = tasks.stream()
                    .filter(t -> t.getStatus() != TaskStatus.DONE)
                    .toList();

            List<Map<String, Object>> items = activeTasks.stream()
                    .map(this::taskToMap)
                    .toList();

            String summary = buildTasksSummary(activeTasks, targetDate);

            String actionUrl = organizationId != null
                    ? "/org/" + organizationId + "/tasks"
                    : "/personal/dashboard";

            return ChatbotResponse.builder()
                    .intent(ChatbotIntent.QUERY_TASKS)
                    .entities(buildEntities(parsed))
                    .confirmationText(summary)
                    .actionUrl(actionUrl)
                    .actionExecuted(true)
                    .queryResults(ChatbotResponse.QueryResults.builder()
                            .totalCount(activeTasks.size())
                            .items(items)
                            .summary(summary)
                            .build())
                    .quickActions(List.of(
                            ChatbotResponse.QuickAction.builder()
                                    .label("Voir toutes les taches")
                                    .action("navigate")
                                    .url(actionUrl)
                                    .build()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to query tasks from chatbot", e);
            return buildErrorResponse(parsed, "Erreur lors de la recuperation des taches: " + e.getMessage());
        }
    }

    private ChatbotResponse executeQueryStats(ChatbotParsedMessage parsed, UUID userId) {
        try {
            ProductivityStatsResponse stats = productivityStatsService.getProductivityStats(userId);

            String summary = String.format(
                    "Cette semaine: %d taches completees (score de productivite: %d/100). " +
                    "Ce mois: %d taches completees. Serie actuelle: %d jours.",
                    stats.getTasksCompletedThisWeek(),
                    stats.getProductivityScore(),
                    stats.getTasksCompletedThisMonth(),
                    stats.getProductiveStreak()
            );

            Map<String, Object> statsMap = new HashMap<>();
            statsMap.put("tasksCompletedThisWeek", stats.getTasksCompletedThisWeek());
            statsMap.put("tasksCompletedThisMonth", stats.getTasksCompletedThisMonth());
            statsMap.put("productivityScore", stats.getProductivityScore());
            statsMap.put("currentStreak", stats.getProductiveStreak());

            return ChatbotResponse.builder()
                    .intent(ChatbotIntent.QUERY_STATS)
                    .entities(buildEntities(parsed))
                    .confirmationText(summary)
                    .actionUrl("/personal/dashboard")
                    .actionExecuted(true)
                    .queryResults(ChatbotResponse.QueryResults.builder()
                            .totalCount(1)
                            .items(List.of(statsMap))
                            .summary(summary)
                            .build())
                    .quickActions(List.of(
                            ChatbotResponse.QuickAction.builder()
                                    .label("Voir le dashboard")
                                    .action("navigate")
                                    .url("/personal/dashboard")
                                    .build()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to query stats from chatbot", e);
            return buildErrorResponse(parsed, "Erreur lors de la recuperation des statistiques: " + e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private String normalizeMessage(String message) {
        // Normalize accents and lowercase for pattern matching
        return message.toLowerCase()
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace("â", "a")
                .replace("ù", "u")
                .replace("û", "u")
                .replace("ô", "o")
                .replace("î", "i")
                .replace("ï", "i")
                .replace("ç", "c");
    }

    private DayOfWeek parseDayOfWeek(String day) {
        String normalized = day.toLowerCase();
        return switch (normalized) {
            case "lundi" -> DayOfWeek.MONDAY;
            case "mardi" -> DayOfWeek.TUESDAY;
            case "mercredi" -> DayOfWeek.WEDNESDAY;
            case "jeudi" -> DayOfWeek.THURSDAY;
            case "vendredi" -> DayOfWeek.FRIDAY;
            case "samedi" -> DayOfWeek.SATURDAY;
            case "dimanche" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private int parseMonth(String month) {
        String normalized = month.toLowerCase()
                .replace("é", "e")
                .replace("û", "u");
        return switch (normalized) {
            case "janvier" -> 1;
            case "fevrier" -> 2;
            case "mars" -> 3;
            case "avril" -> 4;
            case "mai" -> 5;
            case "juin" -> 6;
            case "juillet" -> 7;
            case "aout" -> 8;
            case "septembre" -> 9;
            case "octobre" -> 10;
            case "novembre" -> 11;
            case "decembre" -> 12;
            default -> LocalDate.now().getMonthValue();
        };
    }

    private LocalDateTime buildDateTime(LocalDate date, LocalTime time) {
        if (date == null && time == null) {
            return null;
        }
        LocalDate d = date != null ? date : LocalDate.now();
        LocalTime t = time != null ? time : LocalTime.of(9, 0);
        return LocalDateTime.of(d, t);
    }

    private double calculateConfidence(ChatbotIntent intent, LocalDate date, LocalTime time,
                                        TaskPriority priority, String title) {
        if (intent == ChatbotIntent.UNKNOWN) {
            return 0.0;
        }

        double confidence = 0.5; // Base confidence for detected intent

        if (title != null && !title.isEmpty()) {
            confidence += 0.2;
        }
        if (date != null) {
            confidence += 0.15;
        }
        if (time != null) {
            confidence += 0.1;
        }
        if (priority != TaskPriority.MEDIUM) {
            confidence += 0.05;
        }

        return Math.min(confidence, 1.0);
    }

    private ChatbotResponse.ExtractedEntities buildEntities(ChatbotParsedMessage parsed) {
        return ChatbotResponse.ExtractedEntities.builder()
                .title(parsed.getTitle())
                .description(parsed.getDescription())
                .date(parsed.getExtractedDate())
                .time(parsed.getExtractedTime())
                .priority(parsed.getPriority())
                .build();
    }

    private String buildTaskConfirmation(TaskResponse task) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tache \"").append(task.getTitle()).append("\" creee avec succes.");

        if (task.getDueDate() != null) {
            sb.append(" Echeance: ").append(formatDate(task.getDueDate().toLocalDate())).append(".");
        }

        if (task.getPriority() != null && task.getPriority() != TaskPriority.MEDIUM) {
            sb.append(" Priorite: ").append(formatPriority(task.getPriority())).append(".");
        }

        return sb.toString();
    }

    private String buildEventConfirmation(EventResponse event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Evenement \"").append(event.getTitle()).append("\" cree avec succes.");

        if (event.getStartTime() != null) {
            sb.append(" Date: ").append(formatDateTime(event.getStartTime())).append(".");
        }

        return sb.toString();
    }

    private String buildGoalConfirmation(GoalResponse goal) {
        StringBuilder sb = new StringBuilder();
        sb.append("Objectif \"").append(goal.getTitle()).append("\" cree avec succes.");

        if (goal.getDeadline() != null) {
            sb.append(" Echeance: ").append(formatDate(goal.getDeadline())).append(".");
        }

        return sb.toString();
    }

    private String buildTasksSummary(List<Task> tasks, LocalDate targetDate) {
        if (tasks.isEmpty()) {
            if (targetDate != null) {
                return "Aucune tache prevue pour le " + formatDate(targetDate) + ".";
            }
            return "Aucune tache en cours.";
        }

        StringBuilder sb = new StringBuilder();
        if (targetDate != null) {
            sb.append("Vous avez ").append(tasks.size()).append(" tache(s) pour le ").append(formatDate(targetDate)).append(":");
        } else {
            sb.append("Vous avez ").append(tasks.size()).append(" tache(s) en cours:");
        }

        int shown = Math.min(tasks.size(), 5);
        for (int i = 0; i < shown; i++) {
            Task task = tasks.get(i);
            sb.append("\n- ").append(task.getTitle());
            if (task.getDueDate() != null && targetDate == null) {
                sb.append(" (").append(formatDate(task.getDueDate().toLocalDate())).append(")");
            }
        }

        if (tasks.size() > 5) {
            sb.append("\n... et ").append(tasks.size() - 5).append(" autre(s).");
        }

        return sb.toString();
    }

    private Map<String, Object> taskToMap(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId().toString());
        map.put("title", task.getTitle());
        map.put("status", task.getStatus().name());
        map.put("priority", task.getPriority() != null ? task.getPriority().name() : "MEDIUM");
        map.put("dueDate", task.getDueDate() != null ? task.getDueDate().toLocalDate().toString() : null);
        return map;
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String formatPriority(TaskPriority priority) {
        return switch (priority) {
            case URGENT -> "Urgente";
            case HIGH -> "Haute";
            case MEDIUM -> "Moyenne";
            case LOW -> "Basse";
        };
    }

    private List<ChatbotResponse.QuickAction> buildTaskQuickActions(UUID taskId, UUID orgId) {
        return List.of(
                ChatbotResponse.QuickAction.builder()
                        .label("Voir la tache")
                        .action("navigate")
                        .url("/org/" + orgId + "/tasks")
                        .build(),
                ChatbotResponse.QuickAction.builder()
                        .label("Creer une autre")
                        .action("create_task")
                        .url(null)
                        .build()
        );
    }

    private List<ChatbotResponse.QuickAction> buildEventQuickActions(UUID eventId, UUID orgId) {
        String url = orgId != null ? "/org/" + orgId + "/calendar" : "/personal/calendar";
        return List.of(
                ChatbotResponse.QuickAction.builder()
                        .label("Voir le calendrier")
                        .action("navigate")
                        .url(url)
                        .build(),
                ChatbotResponse.QuickAction.builder()
                        .label("Creer un autre")
                        .action("create_event")
                        .url(null)
                        .build()
        );
    }

    private List<ChatbotResponse.QuickAction> buildGoalQuickActions(UUID goalId, UUID orgId) {
        String url = orgId != null ? "/org/" + orgId + "/goals" : "/personal/goals";
        return List.of(
                ChatbotResponse.QuickAction.builder()
                        .label("Voir les objectifs")
                        .action("navigate")
                        .url(url)
                        .build(),
                ChatbotResponse.QuickAction.builder()
                        .label("Creer un autre")
                        .action("create_goal")
                        .url(null)
                        .build()
        );
    }

    private List<ChatbotResponse.QuickAction> buildNoteQuickActions(UUID noteId, UUID orgId) {
        return List.of(
                ChatbotResponse.QuickAction.builder()
                        .label("Voir les notes")
                        .action("navigate")
                        .url("/org/" + orgId + "/notes")
                        .build(),
                ChatbotResponse.QuickAction.builder()
                        .label("Creer une autre")
                        .action("create_note")
                        .url(null)
                        .build()
        );
    }

    private ChatbotResponse buildUnknownResponse(ChatbotParsedMessage parsed) {
        return ChatbotResponse.builder()
                .intent(ChatbotIntent.UNKNOWN)
                .entities(buildEntities(parsed))
                .confirmationText("Je n'ai pas compris votre demande. Essayez par exemple:\n" +
                        "- \"Creer une tache: finir le rapport\"\n" +
                        "- \"J'ai un rdv demain a 14h avec le client\"\n" +
                        "- \"Note: idee pour le projet\"\n" +
                        "- \"Quelles sont mes taches aujourd'hui?\"\n" +
                        "- \"Combien de taches j'ai completees?\"")
                .actionExecuted(false)
                .quickActions(List.of(
                        ChatbotResponse.QuickAction.builder()
                                .label("Creer une tache")
                                .action("create_task")
                                .url(null)
                                .build(),
                        ChatbotResponse.QuickAction.builder()
                                .label("Creer un evenement")
                                .action("create_event")
                                .url(null)
                                .build(),
                        ChatbotResponse.QuickAction.builder()
                                .label("Voir mes taches")
                                .action("query_tasks")
                                .url(null)
                                .build()
                ))
                .build();
    }

    private ChatbotResponse buildErrorResponse(ChatbotParsedMessage parsed, String errorMessage) {
        return ChatbotResponse.builder()
                .intent(parsed.getIntent())
                .entities(buildEntities(parsed))
                .confirmationText(errorMessage)
                .actionExecuted(false)
                .errorMessage(errorMessage)
                .quickActions(List.of())
                .build();
    }
}
