package com.uppdragsradarn.infrastructure.crawler.providers;

import java.util.*;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uppdragsradarn.application.service.LocationService;
import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.CrawlerException;
import com.uppdragsradarn.domain.model.Currency;
import com.uppdragsradarn.domain.model.Skill;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.StatusType;
import com.uppdragsradarn.domain.repository.CurrencyRepository;
import com.uppdragsradarn.domain.repository.SkillRepository;
import com.uppdragsradarn.domain.repository.SourceTypeRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;

import lombok.extern.slf4j.Slf4j;

/** Content provider for Experis jobs from their REST API */
@Component
@Slf4j
public class ExperisProvider extends AbstractProvider {

  private static final String API_ENDPOINT = "/api/services/Jobs/searchjobs";
  private static final String JOB_DETAIL_BASE_URL = "https://www.experis.se/sv/jobb/";

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final int timeoutSeconds;
  private final String userAgent;
  private final int maxAssignments;
  private final String baseUrl;
  private final int pageSize;

  private final LocationService locationService;
  private final SkillRepository skillRepository;
  private final CurrencyRepository currencyRepository;
  private final SourceTypeRepository sourceTypeRepository;
  private final StatusTypeRepository statusTypeRepository;

  @Override
  public String getName() {
    return "Experis Provider";
  }

  public ExperisProvider(
      @Value("${app.crawler.timeout:30}") int timeoutSeconds,
      @Value(
              "${app.crawler.user-agent:Mozilla/5.0 (compatible; UppdragsRadarn/1.0; +https://uppdragsradarn.se/bot)}")
          String userAgent,
      @Value("${app.crawler.max-assignments:200}") int maxAssignments,
      @Value("${app.crawler.experis.url:https://www.experis.se}") String baseUrl,
      @Value("${app.crawler.experis.page-size:20}") int pageSize,
      LocationService locationService,
      SkillRepository skillRepository,
      CurrencyRepository currencyRepository,
      SourceTypeRepository sourceTypeRepository,
      StatusTypeRepository statusTypeRepository) {
    this.timeoutSeconds = timeoutSeconds;
    this.userAgent = userAgent;
    this.maxAssignments = maxAssignments;
    this.baseUrl = baseUrl;
    this.pageSize = pageSize;
    this.locationService = locationService;
    this.skillRepository = skillRepository;
    this.currencyRepository = currencyRepository;
    this.sourceTypeRepository = sourceTypeRepository;
    this.statusTypeRepository = statusTypeRepository;
  }

  @Override
  public boolean supports(Source source) {
    return source.getName().equalsIgnoreCase("Experis")
        || (source.getSourceType() != null
            && "API".equals(source.getSourceType().getName())
            && source.getBaseUrl() != null
            && source.getBaseUrl().contains("experis"));
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    logger.info("Fetching assignments from Experis API: {}", baseUrl);

    List<Assignment> allAssignments = new ArrayList<>();
    int page = 1;
    boolean hasMore = true;

    try (CloseableHttpClient httpClient = createHttpClient()) {
      while (hasMore && allAssignments.size() < maxAssignments) {
        try {
          // Get assignments for current page
          List<Assignment> pageAssignments = fetchAssignmentsForPage(httpClient, page, source);

          if (pageAssignments.isEmpty()) {
            hasMore = false;
          } else {
            allAssignments.addAll(pageAssignments);
            page++;
          }
        } catch (CrawlerException e) {
          // If this is a 500 error on the first page, the API is likely down
          if (page == 1 && e.getMessage().contains("Status code: 500")) {
            logger.warn("Experis API appears to be down (500 error). Skipping this crawl cycle.");
            return Collections.emptyList(); // Return empty list instead of failing
          } else {
            // For other errors or later pages, propagate the exception
            throw e;
          }
        }
      }

      logger.info("Successfully fetched {} assignments from Experis", allAssignments.size());
      return allAssignments;
    } catch (Exception e) {
      logger.error("Error fetching assignments from Experis: {}", e.getMessage(), e);
      throw new CrawlerException("Error fetching assignments: " + e.getMessage(), e);
    }
  }

  /** Fetches a page of assignments from the Experis API */
  private List<Assignment> fetchAssignmentsForPage(
      CloseableHttpClient httpClient, int page, Source source) throws CrawlerException {
    try {
      // Build URL with query parameters
      String apiUrl = baseUrl + API_ENDPOINT;
      logger.debug("Making request to API endpoint: {}, page: {}", apiUrl, page);

      // Create and configure POST request
      HttpPost httpPost = new HttpPost(apiUrl);
      httpPost.setHeader("User-Agent", userAgent);
      httpPost.setHeader("Content-Type", "application/json");
      httpPost.setHeader("Accept", "application/json");

      // Create request payload
      ObjectNode requestJson = objectMapper.createObjectNode();
      requestJson.put("City", "");
      requestJson.put("Keywords", "");
      requestJson.put("SkipCount", (page - 1) * pageSize);
      requestJson.put("MaxResultCount", pageSize);
      requestJson.put("IsFullyRemote", false);
      requestJson.put("EduLevels", "");
      requestJson.put("ResultsPerPage", pageSize);
      requestJson.put("Country", "SE");
      requestJson.put("Language", "sv");

      // Set POST data as JSON
      httpPost.setEntity(new StringEntity(requestJson.toString(), ContentType.APPLICATION_JSON));

      // Execute request
      try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
        int statusCode = response.getCode();
        if (statusCode != 200) {
          // Try to get error details from response body
          String errorDetails = "";
          try {
            HttpEntity errorEntity = response.getEntity();
            if (errorEntity != null) {
              errorDetails = EntityUtils.toString(errorEntity);
              logger.warn("Experis API error response body: {}", errorDetails);
            }
          } catch (Exception e) {
            logger.debug("Could not read error response body: {}", e.getMessage());
          }

          logger.warn(
              "Failed to fetch assignments from {}: Status code: {}, URL: {}",
              baseUrl,
              statusCode,
              apiUrl);
          throw new CrawlerException(
              "Failed to fetch assignments. Status code: "
                  + statusCode
                  + (errorDetails.isEmpty() ? "" : ", Error: " + errorDetails));
        }

        // Parse response
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          String jsonResponse = EntityUtils.toString(entity);
          return parseJobListings(jsonResponse, source);
        } else {
          logger.warn("Empty response from Experis API");
          return Collections.emptyList();
        }
      }
    } catch (Exception e) {
      logger.error("Error fetching page {} from Experis: {}", page, e.getMessage(), e);
      throw new CrawlerException(
          "Error fetching page " + page + " from Experis: " + e.getMessage(), e);
    }
  }

  /** Creates an HTTP client with timeouts configured */
  private CloseableHttpClient createHttpClient() {
    RequestConfig config =
        RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(timeoutSeconds))
            .setResponseTimeout(Timeout.ofSeconds(timeoutSeconds))
            .build();

    return HttpClients.custom().setDefaultRequestConfig(config).build();
  }

  /** Parses JSON response from API into Assignment objects */
  private List<Assignment> parseJobListings(String jsonResponse, Source source)
      throws CrawlerException {
    try {
      JsonNode rootNode = objectMapper.readTree(jsonResponse);
      JsonNode itemsNode = rootNode.path("items");

      if (!itemsNode.isArray()) {
        logger.warn("Invalid response format: 'items' is not an array");
        return Collections.emptyList();
      }

      List<Assignment> assignments = new ArrayList<>();
      for (JsonNode jobNode : itemsNode) {
        try {
          Assignment assignment = createAssignmentFromJson(jobNode, source);
          assignments.add(assignment);
        } catch (Exception e) {
          logger.warn("Error parsing job from JSON: {}", e.getMessage());
        }
      }

      return assignments;
    } catch (Exception e) {
      logger.error("Error parsing job listings from JSON: {}", e.getMessage(), e);
      throw new CrawlerException("Error parsing job listings from JSON: " + e.getMessage(), e);
    }
  }

  /** Creates an Assignment object from a JSON job node */
  private Assignment createAssignmentFromJson(JsonNode jobNode, Source source) {
    // Extract required fields
    String title = jobNode.path("title").asText("");
    String externalId = jobNode.path("uniqueId").asText("");
    String applicationUrl = JOB_DETAIL_BASE_URL + externalId;

    // Create assignment
    Assignment assignment = new Assignment();
    assignment.setTitle(title);
    assignment.setExternalId(externalId);
    assignment.setApplicationUrl(applicationUrl);
    assignment.setSource(source);
    assignment.setActive(true);

    // Set default status to ACTIVE
    StatusType activeStatus = getOrCreateStatusType("ACTIVE", "ASSIGNMENT");
    assignment.setStatus(activeStatus);

    // Extract and set location
    String location = jobNode.path("location").asText("");
    if (location != null && !location.isEmpty()) {
      locationService.processAssignmentLocation(assignment, location, source.getName());
    }

    // Extract company name
    String companyName = jobNode.path("employerName").asText(source.getName());
    assignment.setCompanyName(companyName);

    // Generate description from available fields
    StringBuilder descBuilder = new StringBuilder();
    descBuilder.append(jobNode.path("summary").asText("")).append("\n\n");

    // Add any additional details available in the data
    if (location != null && !location.isEmpty()) {
      // Use assignment locations but format them for display if needed
      Optional<String> formattedLocation = getFormattedLocationText(assignment);
      if (formattedLocation.isPresent()) {
        descBuilder.append("Location: ").append(formattedLocation.get()).append("\n");
      }
    }

    // Set description
    assignment.setDescription(descBuilder.toString().trim());

    // Process categories as skills
    JsonNode categoriesNode = jobNode.path("categories");
    if (categoriesNode != null && categoriesNode.isArray() && !categoriesNode.isEmpty()) {
      Set<String> skillNames = new HashSet<>();
      for (JsonNode categoryNode : categoriesNode) {
        String category = categoryNode.asText();
        if (category != null && !category.isEmpty()) {
          skillNames.add(category);
        }
      }
      processSkills(assignment, skillNames);
    }

    return assignment;
  }

  /** Helper method to get a formatted location string from assignment locations */
  private Optional<String> getFormattedLocationText(Assignment assignment) {
    // If we have assignment locations, try to get original text from them
    if (assignment.getAssignmentLocations() != null
        && !assignment.getAssignmentLocations().isEmpty()) {
      return assignment.getAssignmentLocations().stream()
          .filter(al -> al.getOriginalText() != null && !al.getOriginalText().isEmpty())
          .map(al -> al.getOriginalText())
          .findFirst();
    }
    return Optional.empty();
  }

  /** Processes skills and adds them to assignment */
  private void processSkills(Assignment assignment, Set<String> skillNames) {
    for (String skillName : skillNames) {
      Skill skill = findOrCreateSkill(skillName);
      assignment.addSkill(skill);
    }
  }

  /** Finds or creates a skill entity */
  private Skill findOrCreateSkill(String skillName) {
    try {
      return skillRepository
          .findByNameIgnoreCase(skillName)
          .orElseGet(
              () -> {
                try {
                  Skill newSkill = Skill.builder().name(skillName).build();
                  return skillRepository.save(newSkill);
                } catch (Exception e) {
                  // If save fails (likely due to unique constraint), try to find it again
                  return skillRepository
                      .findByNameIgnoreCase(skillName)
                      .orElseThrow(
                          () ->
                              new RuntimeException(
                                  "Failed to create or find skill: " + skillName, e));
                }
              });
    } catch (Exception e) {
      logger.warn("Error in findOrCreateSkill for skill {}: {}", skillName, e.getMessage());
      // Last attempt - try to get by name one more time
      return skillRepository
          .findByNameIgnoreCase(skillName)
          .orElseThrow(
              () ->
                  new RuntimeException(
                      "Failed to create or find skill after retrying: " + skillName, e));
    }
  }

  /** Sets currency on assignment */
  private void setCurrency(Assignment assignment, String currencyCode) {
    Currency currency = findOrCreateCurrency(currencyCode);
    assignment.setCurrency(currency);
  }

  /** Finds or creates currency entity */
  private Currency findOrCreateCurrency(String code) {
    return currencyRepository
        .findByCode(code)
        .orElseGet(
            () -> {
              Currency newCurrency =
                  Currency.builder()
                      .code(code)
                      .name(getCurrencyName(code))
                      .symbol(getCurrencySymbol(code))
                      .build();
              return currencyRepository.save(newCurrency);
            });
  }

  /** Gets currency name based on code */
  private String getCurrencyName(String code) {
    switch (code) {
      case "SEK":
        return "Swedish Krona";
      case "EUR":
        return "Euro";
      case "USD":
        return "US Dollar";
      case "GBP":
        return "British Pound";
      case "NOK":
        return "Norwegian Krone";
      case "DKK":
        return "Danish Krone";
      default:
        return code;
    }
  }

  /** Gets currency symbol based on code */
  private String getCurrencySymbol(String code) {
    switch (code) {
      case "SEK":
        return "kr";
      case "EUR":
        return "€";
      case "USD":
        return "$";
      case "GBP":
        return "£";
      case "NOK":
        return "kr";
      case "DKK":
        return "kr";
      default:
        return code;
    }
  }

  /** Helper method to get or create a status type */
  private StatusType getOrCreateStatusType(String name, String entityType) {
    return statusTypeRepository
        .findByNameAndEntityType(name, entityType)
        .orElseGet(
            () -> {
              StatusType newStatus = StatusType.builder().name(name).entityType(entityType).build();
              return statusTypeRepository.save(newStatus);
            });
  }
}
