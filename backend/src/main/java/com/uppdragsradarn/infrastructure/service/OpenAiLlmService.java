package com.uppdragsradarn.infrastructure.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uppdragsradarn.domain.model.Assignment;

import lombok.Data;

/**
 * Service for integrating with OpenAI's GPT-4o-mini model for job detail extraction.
 * Provides async and sync methods for processing HTML content into structured assignment data.
 */
@Service
public class OpenAiLlmService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAiLlmService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    @Value("${app.openai.api-key:}")
    private String apiKey;
    
    @Value("${app.openai.model:gpt-4o-mini}")
    private String defaultModel;
    
    @Value("${app.openai.timeout-seconds:30}")
    private int timeoutSeconds;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    
    public OpenAiLlmService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    /**
     * Extracts structured assignment data from HTML content using OpenAI LLM.
     * 
     * @param htmlContent The filtered HTML content to process
     * @param model The OpenAI model to use (default: gpt-4o-mini)
     * @param customInstructions Additional provider-specific instructions
     * @return ExtractedAssignmentData containing structured job information
     */
    public CompletableFuture<ExtractedAssignmentData> extractAssignmentDataAsync(
            String htmlContent, String model, String customInstructions) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractAssignmentData(htmlContent, model, customInstructions);
            } catch (Exception e) {
                logger.error("Error in async LLM extraction: {}", e.getMessage(), e);
                return new ExtractedAssignmentData(); // Return empty data on error
            }
        });
    }
    
    /**
     * Synchronous version of extractAssignmentDataAsync.
     */
    public ExtractedAssignmentData extractAssignmentData(
            String htmlContent, String model, String customInstructions) throws Exception {
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API key not configured");
        }
        
        String effectiveModel = model != null ? model : defaultModel;
        String prompt = buildExtractionPrompt(htmlContent, customInstructions);
        
        OpenAiRequest request = new OpenAiRequest();
        request.setModel(effectiveModel);
        request.setTemperature(0.1);
        request.setMaxTokens(800);
        request.getMessages().add(new OpenAiMessage("system", getSystemPrompt()));
        request.getMessages().add(new OpenAiMessage("user", prompt));
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }
        
        OpenAiResponse openAiResponse = objectMapper.readValue(response.body(), OpenAiResponse.class);
        
        if (openAiResponse.getChoices() == null || openAiResponse.getChoices().isEmpty()) {
            throw new RuntimeException("No response from OpenAI API");
        }
        
        String content = openAiResponse.getChoices().get(0).getMessage().getContent();
        
        // Parse the JSON response into our data structure
        try {
            return objectMapper.readValue(content, ExtractedAssignmentData.class);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse LLM JSON response, returning partial data: {}", e.getMessage());
            // Return basic data if JSON parsing fails
            ExtractedAssignmentData fallback = new ExtractedAssignmentData();
            fallback.setDescription(content); // At least return the raw content
            return fallback;
        }
    }
    
    /**
     * Estimates the token count for the given text (rough approximation).
     */
    public int estimateTokenCount(String text) {
        if (text == null) return 0;
        // Rough estimate: 1 token ≈ 4 characters for English text
        return text.length() / 4;
    }
    
    private String getSystemPrompt() {
        return """
            You are an expert at extracting structured job assignment information from HTML content.
            
            Your task is to analyze the provided HTML content and extract job assignment details into a JSON format.
            
            Return ONLY valid JSON with the following structure:
            {
              "title": "Job title",
              "description": "Clean job description in markdown format",
              "companyName": "Company name",
              "location": "Location (city, country)",
              "hourlyRateMin": 750,
              "hourlyRateMax": 950,
              "currency": "SEK",
              "durationMonths": 6,
              "hoursPerWeek": 40,
              "startDate": "2024-02-01",
              "applicationDeadline": "2024-01-15",
              "skills": ["Java", "Spring Boot", "React"],
              "workArrangement": "Remote/Hybrid/On-site",
              "requirementLevel": "Senior/Mid/Junior",
              "applicationUrl": "https://...",
              "externalId": "job-123"
            }
            
            Rules:
            - Convert HTML to clean markdown for description
            - Extract numeric values for rates, duration, hours
            - Parse dates in ISO format (YYYY-MM-DD)
            - Skills should be an array of individual technologies/skills
            - Use null for missing information
            - Currency codes: SEK, EUR, USD, etc.
            - Location should be normalized (e.g., "Stockholm, Sweden")
            """;
    }
    
    private String buildExtractionPrompt(String htmlContent, String customInstructions) {
        StringBuilder prompt = new StringBuilder();
        
        if (customInstructions != null && !customInstructions.isEmpty()) {
            prompt.append("Additional context: ").append(customInstructions).append("\n\n");
        }
        
        prompt.append("Extract job assignment information from this HTML content:\n\n");
        prompt.append(htmlContent);
        
        return prompt.toString();
    }
    
    // Data classes for OpenAI API communication
    @Data
    public static class OpenAiRequest {
        private String model;
        private java.util.List<OpenAiMessage> messages = new java.util.ArrayList<>();
        private double temperature;
        @JsonProperty("max_tokens")
        private int maxTokens;
    }
    
    @Data
    public static class OpenAiMessage {
        private String role;
        private String content;
        
        public OpenAiMessage() {}
        
        public OpenAiMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAiResponse {
        private java.util.List<Choice> choices;
        private Usage usage;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {
            private OpenAiMessage message;
            @JsonProperty("finish_reason")
            private String finishReason;
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Usage {
            @JsonProperty("prompt_tokens")
            private int promptTokens;
            @JsonProperty("completion_tokens")
            private int completionTokens;
            @JsonProperty("total_tokens")
            private int totalTokens;
        }
    }
    
    // Data structure for extracted assignment information
    @Data
    public static class ExtractedAssignmentData {
        private String title;
        private String description;
        private String companyName;
        private String location;
        private Integer hourlyRateMin;
        private Integer hourlyRateMax;
        private String currency;
        private Integer durationMonths;
        private Integer hoursPerWeek;
        private String startDate; // ISO format
        private String applicationDeadline; // ISO format
        private java.util.List<String> skills;
        private String workArrangement;
        private String requirementLevel;
        private String applicationUrl;
        private String externalId;
        
        /**
         * Applies the extracted data to an Assignment entity.
         */
        public void applyToAssignment(Assignment assignment) {
            if (title != null) assignment.setTitle(title);
            if (description != null) assignment.setDescription(description);
            if (companyName != null) assignment.setCompanyName(companyName);
            if (hourlyRateMin != null) assignment.setHourlyRateMin(new java.math.BigDecimal(hourlyRateMin));
            if (hourlyRateMax != null) assignment.setHourlyRateMax(new java.math.BigDecimal(hourlyRateMax));
            if (durationMonths != null) assignment.setDurationMonths(durationMonths);
            if (hoursPerWeek != null) assignment.setHoursPerWeek(hoursPerWeek);
            if (applicationUrl != null) assignment.setApplicationUrl(applicationUrl);
            if (externalId != null) assignment.setExternalId(externalId);
            
            // Parse dates
            if (startDate != null) {
                try {
                    assignment.setStartDate(java.time.LocalDate.parse(startDate));
                } catch (Exception e) {
                    logger.warn("Failed to parse start date: {}", startDate);
                }
            }
            
            if (applicationDeadline != null) {
                try {
                    assignment.setApplicationDeadline(java.time.LocalDate.parse(applicationDeadline));
                } catch (Exception e) {
                    logger.warn("Failed to parse application deadline: {}", applicationDeadline);
                }
            }
        }
    }
}