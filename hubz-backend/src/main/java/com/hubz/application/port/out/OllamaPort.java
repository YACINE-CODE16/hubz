package com.hubz.application.port.out;

/**
 * Port interface for communication with Ollama LLM API.
 * Follows Clean Architecture - this interface is in the application layer,
 * and its implementation (adapter) is in the infrastructure layer.
 */
public interface OllamaPort {

    /**
     * Generate a response from Ollama using the configured model.
     *
     * @param prompt       The user prompt to send
     * @param systemPrompt The system prompt providing context and instructions
     * @return The generated response text, or null if generation fails
     */
    String generateResponse(String prompt, String systemPrompt);

    /**
     * Generate a response with conversation history context.
     *
     * @param prompt             The user prompt to send
     * @param systemPrompt       The system prompt providing context and instructions
     * @param conversationHistory Previous messages in the conversation for context
     * @return The generated response text, or null if generation fails
     */
    String generateResponseWithHistory(String prompt, String systemPrompt, String conversationHistory);

    /**
     * Check if Ollama service is available and responding.
     *
     * @return true if Ollama is available, false otherwise
     */
    boolean isAvailable();

    /**
     * Get the name of the currently configured model.
     *
     * @return The model name (e.g., "llama3.1")
     */
    String getModelName();
}
