package com.uppdragsradarn.infrastructure.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.uppdragsradarn.application.service.LocationService;
import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.Currency;
import com.uppdragsradarn.domain.model.Skill;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.StatusType;
import com.uppdragsradarn.domain.repository.CurrencyRepository;
import com.uppdragsradarn.domain.repository.SkillRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;
import com.uppdragsradarn.infrastructure.crawler.config.ExtractionConfig;
import com.uppdragsradarn.infrastructure.service.OpenAiLlmService.ExtractedAssignmentData;

import lombok.extern.slf4j.Slf4j;

/**
 * Main service for LLM-based job detail extraction.
 * Orchestrates the process of fetching job pages, filtering content, and extracting structured data.
 */
@Service
@Slf4j
public class LlmJobExtractionService {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmJobExtractionService.class);
    
    @Value("${app.crawler.user-agent:Mozilla/5.0 (compatible; UppdragsRadarn/1.0; +https://uppdragsradarn.se/bot)}")
    private String userAgent;
    
    @Value("${app.crawler.timeout:30}")
    private int timeoutSeconds;
    
    private final OpenAiLlmService llmService;
    private final HtmlContentFilterService contentFilterService;
    private final LocationService locationService;
    private final SkillRepository skillRepository;
    private final CurrencyRepository currencyRepository;
    private final StatusTypeRepository statusTypeRepository;
    private final HttpClient httpClient;
    
    public LlmJobExtractionService(
            OpenAiLlmService llmService,
            HtmlContentFilterService contentFilterService,
            LocationService locationService,
            SkillRepository skillRepository,
            CurrencyRepository currencyRepository,
            StatusTypeRepository statusTypeRepository) {
        this.llmService = llmService;
        this.contentFilterService = contentFilterService;
        this.locationService = locationService;
        this.skillRepository = skillRepository;
        this.currencyRepository = currencyRepository;
        this.statusTypeRepository = statusTypeRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(timeoutSeconds, 5))) // Ensure minimum 5 seconds
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
    
    /**
     * Extracts detailed assignment information from a job URL using LLM processing.
     * 
     * @param jobUrl The URL of the job detail page
     * @param source The source this job belongs to
     * @param providerId Provider identifier for configuration lookup
     * @return Assignment with LLM-extracted details, or null if extraction fails
     */
    public Assignment extractAssignmentFromUrl(String jobUrl, Source source, String providerId) {
        try {
            logger.debug("Extracting assignment from URL: {}", jobUrl);
            
            // Step 1: Load extraction configuration for this provider
            ExtractionConfig config = contentFilterService.loadExtractionConfig(providerId);
            
            // Step 2: Fetch the HTML content
            String htmlContent = fetchJobPageContent(jobUrl);
            if (htmlContent == null || htmlContent.isEmpty()) {
                logger.warn("No content fetched from URL: {}", jobUrl);
                return null;
            }
            
            // Step 3: Filter and optimize content for LLM
            String filteredContent = contentFilterService.filterContent(htmlContent, config);
            
            // Log token usage for monitoring costs
            int tokenCount = contentFilterService.estimateTokenCount(filteredContent);
            logger.debug("Filtered content token count: {} for URL: {}", tokenCount, jobUrl);
            
            // Step 4: Extract structured data using LLM
            ExtractedAssignmentData extractedData = llmService.extractAssignmentData(
                filteredContent, 
                config.getLlmConfig().getModel(), 
                config.getLlmConfig().getCustomInstructions()
            );
            
            // Step 5: Create and populate Assignment entity
            Assignment assignment = createAssignmentFromExtractedData(extractedData, jobUrl, source);
            
            logger.info("Successfully extracted assignment: {} from {}", assignment.getTitle(), jobUrl);
            return assignment;
            
        } catch (Exception e) {
            logger.error("Failed to extract assignment from URL {}: {}", jobUrl, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Async version of extractAssignmentFromUrl for batch processing.
     */
    public CompletableFuture<Assignment> extractAssignmentFromUrlAsync(
            String jobUrl, Source source, String providerId) {
        
        return CompletableFuture.supplyAsync(() -> 
            extractAssignmentFromUrl(jobUrl, source, providerId)
        );
    }
    
    /**
     * Processes a batch of job URLs concurrently.
     */
    public CompletableFuture<java.util.List<Assignment>> extractAssignmentsBatch(
            java.util.List<String> jobUrls, Source source, String providerId) {
        
        java.util.List<CompletableFuture<Assignment>> futures = jobUrls.stream()
            .map(url -> extractAssignmentFromUrlAsync(url, source, providerId))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(assignment -> assignment != null)
                .toList());
    }
    
    private String fetchJobPageContent(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP error: " + response.statusCode() + " for URL: " + url);
        }
        
        return response.body();
    }
    
    private Assignment createAssignmentFromExtractedData(
            ExtractedAssignmentData data, String jobUrl, Source source) {
        
        Assignment assignment = new Assignment();
        
        // Apply basic extracted data
        data.applyToAssignment(assignment);
        
        // Set required fields
        assignment.setSource(source);
        assignment.setApplicationUrl(jobUrl);
        assignment.setActive(true);
        
        // Generate external ID if not provided
        if (assignment.getExternalId() == null || assignment.getExternalId().isEmpty()) {
            assignment.setExternalId(generateExternalId(jobUrl));
        }
        
        // Set default company name if not extracted
        if (assignment.getCompanyName() == null || assignment.getCompanyName().isEmpty()) {
            assignment.setCompanyName(source.getName());
        }
        
        // Set default status
        StatusType activeStatus = getOrCreateStatusType("ACTIVE", "ASSIGNMENT");
        assignment.setStatus(activeStatus);
        
        // Process currency
        if (data.getCurrency() != null) {
            Currency currency = findOrCreateCurrency(data.getCurrency());
            assignment.setCurrency(currency);
        } else {
            // Default to SEK for Swedish job sites
            Currency sekCurrency = findOrCreateCurrency("SEK");
            assignment.setCurrency(sekCurrency);
        }
        
        // Process location
        if (data.getLocation() != null && !data.getLocation().isEmpty()) {
            locationService.processAssignmentLocation(assignment, data.getLocation(), source.getName());
        }
        
        // Process skills
        if (data.getSkills() != null && !data.getSkills().isEmpty()) {
            processSkills(assignment, new HashSet<>(data.getSkills()));
        }
        
        return assignment;
    }
    
    private String generateExternalId(String url) {
        // Extract last path segment or use hash of URL
        String[] pathParts = url.split("/");
        if (pathParts.length > 0) {
            String lastPart = pathParts[pathParts.length - 1];
            if (lastPart.contains("?")) {
                lastPart = lastPart.substring(0, lastPart.indexOf("?"));
            }
            if (!lastPart.isEmpty() && !lastPart.equals("index.html")) {
                return lastPart;
            }
        }
        return String.valueOf(url.hashCode());
    }
    
    private void processSkills(Assignment assignment, Set<String> skillNames) {
        for (String skillName : skillNames) {
            if (skillName != null && !skillName.trim().isEmpty()) {
                Skill skill = findOrCreateSkill(skillName.trim());
                assignment.addSkill(skill);
            }
        }
    }
    
    private Skill findOrCreateSkill(String skillName) {
        try {
            return skillRepository.findByNameIgnoreCase(skillName)
                .orElseGet(() -> {
                    try {
                        Skill newSkill = Skill.builder().name(skillName).build();
                        return skillRepository.save(newSkill);
                    } catch (Exception e) {
                        // If save fails (likely due to unique constraint), try to find it again
                        return skillRepository.findByNameIgnoreCase(skillName)
                            .orElseThrow(() -> new RuntimeException(
                                "Failed to create or find skill: " + skillName, e));
                    }
                });
        } catch (Exception e) {
            logger.warn("Error in findOrCreateSkill for skill {}: {}", skillName, e.getMessage());
            // Last attempt - try to get by name one more time
            return skillRepository.findByNameIgnoreCase(skillName)
                .orElseThrow(() -> new RuntimeException(
                    "Failed to create or find skill after retrying: " + skillName, e));
        }
    }
    
    private Currency findOrCreateCurrency(String code) {
        try {
            return currencyRepository.findByCode(code)
                .orElseGet(() -> {
                    try {
                        Currency newCurrency = Currency.builder()
                            .code(code)
                            .name(getCurrencyName(code))
                            .symbol(getCurrencySymbol(code))
                            .build();
                        return currencyRepository.save(newCurrency);
                    } catch (Exception e) {
                        // If save fails (likely due to unique constraint), try to find it again
                        return currencyRepository.findByCode(code)
                            .orElseThrow(() -> new RuntimeException(
                                "Failed to create or find currency: " + code, e));
                    }
                });
        } catch (Exception e) {
            logger.warn("Error in findOrCreateCurrency for code {}: {}", code, e.getMessage());
            // Last attempt - try to get by code one more time
            return currencyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException(
                    "Failed to create or find currency after retrying: " + code, e));
        }
    }
    
    private String getCurrencyName(String code) {
        return switch (code) {
            case "SEK" -> "Swedish Krona";
            case "EUR" -> "Euro";
            case "USD" -> "US Dollar";
            case "GBP" -> "British Pound";
            case "NOK" -> "Norwegian Krone";
            case "DKK" -> "Danish Krone";
            default -> code;
        };
    }
    
    private String getCurrencySymbol(String code) {
        return switch (code) {
            case "SEK", "NOK", "DKK" -> "kr";
            case "EUR" -> "€";
            case "USD" -> "$";
            case "GBP" -> "£";
            default -> code;
        };
    }
    
    private StatusType getOrCreateStatusType(String name, String entityType) {
        try {
            return statusTypeRepository.findByNameAndEntityType(name, entityType)
                .orElseGet(() -> {
                    try {
                        StatusType newStatus = StatusType.builder()
                            .name(name)
                            .entityType(entityType)
                            .build();
                        return statusTypeRepository.save(newStatus);
                    } catch (Exception e) {
                        // If save fails (likely due to unique constraint), try to find it again
                        return statusTypeRepository.findByNameAndEntityType(name, entityType)
                            .orElseThrow(() -> new RuntimeException(
                                "Failed to create or find status type: " + name + ":" + entityType, e));
                    }
                });
        } catch (Exception e) {
            logger.warn("Error in getOrCreateStatusType for {}:{}: {}", name, entityType, e.getMessage());
            // Last attempt - try to get by name and type one more time
            return statusTypeRepository.findByNameAndEntityType(name, entityType)
                .orElseThrow(() -> new RuntimeException(
                    "Failed to create or find status type after retrying: " + name + ":" + entityType, e));
        }
    }
}