package com.uppdragsradarn.infrastructure.crawler.providers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Content provider for Ework jobs from Verama platform */
@Component
@Slf4j
public class EworkProvider extends AbstractProvider {

  private static final String API_ENDPOINT = "/api/public/job-requests";
  private static final String JOB_DETAIL_URL = "https://app.verama.com/en/job-requests/";

  private final ObjectMapper objectMapper;
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

  /** Default constructor for Spring dependency injection. */
  public EworkProvider(
      @Value("${app.crawler.timeout:30}") int timeoutSeconds,
      @Value(
              "${app.crawler.user-agent:Mozilla/5.0 (compatible; UppdragsRadarn/1.0; +https://uppdragsradarn.se/bot)}")
          String userAgent,
      @Value("${app.crawler.max-assignments:200}") int maxAssignments,
      @Value("${app.crawler.ework.url:https://app.verama.com}") String baseUrl,
      @Value("${app.crawler.ework.page-size:50}") int pageSize,
      LocationService locationService,
      SkillRepository skillRepository,
      CurrencyRepository currencyRepository,
      SourceTypeRepository sourceTypeRepository,
      StatusTypeRepository statusTypeRepository) {

    this.objectMapper = new ObjectMapper();
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
  public String getName() {
    return "Ework Provider";
  }

  @Override
  public boolean supports(Source source) {
    return source.getName().equalsIgnoreCase("Ework")
        || (source.getSourceType() != null && "EWORK".equals(source.getSourceType().getName()))
        || (source.getBaseUrl() != null && source.getBaseUrl().contains("verama"));
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    logger.info("Fetching assignments from Ework API: {}", baseUrl);

    List<Assignment> allAssignments = new ArrayList<>();
    int page = 0;
    boolean hasMore = true;

    try (CloseableHttpClient httpClient = createHttpClient()) {
      while (hasMore && allAssignments.size() < maxAssignments) {
        // Get assignments for current page
        List<Assignment> pageAssignments = fetchAssignmentsForPage(httpClient, page, source);

        if (pageAssignments.isEmpty()) {
          hasMore = false;
        } else {
          allAssignments.addAll(pageAssignments);
          page++;
        }
      }

      logger.info("Successfully fetched {} assignments from Ework", allAssignments.size());
      return allAssignments;

    } catch (Exception e) {
      logger.error("Error fetching assignments from Ework: {}", e.getMessage(), e);
      throw new CrawlerException("Error fetching assignments from Ework", e);
    }
  }

  /** Creates and configures an HTTP client with appropriate timeouts */
  private CloseableHttpClient createHttpClient() {
    RequestConfig config =
        RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(timeoutSeconds))
            .setResponseTimeout(Timeout.ofSeconds(timeoutSeconds))
            .build();

    return HttpClients.custom().setDefaultRequestConfig(config).build();
  }

  /** Fetches a page of assignments from the API */
  private List<Assignment> fetchAssignmentsForPage(
      CloseableHttpClient httpClient, int page, Source source) throws CrawlerException {
    // Build URL with parameters
    String apiUrl = String.format("%s%s?page=%d&size=%d", baseUrl, API_ENDPOINT, page, pageSize);
    logger.debug("Fetching page {} with URL: {}", page, apiUrl);

    HttpGet httpGet = new HttpGet(apiUrl);
    httpGet.setHeader("User-Agent", userAgent);
    httpGet.setHeader("Accept", "application/json");

    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
      int statusCode = response.getCode();
      if (statusCode != 200) {
        logger.warn("Failed to fetch assignments: Status code: {}", statusCode);
        throw new CrawlerException("Failed to fetch assignments. Status code: " + statusCode);
      }

      HttpEntity entity = response.getEntity();
      if (entity != null) {
        String jsonResponse = EntityUtils.toString(entity);
        return parseJobListings(jsonResponse, source);
      } else {
        logger.warn("Empty response from Ework API");
        return Collections.emptyList();
      }
    } catch (Exception e) {
      logger.error("Error fetching page {} from Ework: {}", page, e.getMessage(), e);
      throw new CrawlerException(
          "Error fetching page " + page + " from Ework: " + e.getMessage(), e);
    }
  }

  /** Parses JSON response from API into Assignment objects */
  protected List<Assignment> parseJobListings(String jsonResponse, Source source)
      throws CrawlerException {
    try {
      JsonNode rootNode = objectMapper.readTree(jsonResponse);
      JsonNode jobsNode = rootNode.path("content");

      if (!jobsNode.isArray()) {
        logger.warn("Invalid response format: 'content' is not an array");
        return Collections.emptyList();
      }

      List<Assignment> assignments = new ArrayList<>();
      for (JsonNode jobNode : jobsNode) {
        try {
          if (jobNode.has("id")) {
            Assignment assignment = createAssignmentFromJson(jobNode, source);
            assignments.add(assignment);
          }
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
    String externalId = jobNode.path("id").asText("");
    String applicationUrl = JOB_DETAIL_URL + externalId;

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

    // Extract company name
    String companyName = jobNode.path("clientName").asText(source.getName());
    assignment.setCompanyName(companyName);

    // Extract description
    String description = jobNode.path("description").asText("");
    assignment.setDescription(description);

    // Extract location
    JsonNode locationsNode = jobNode.path("locations");
    if (locationsNode.isArray() && locationsNode.size() > 0) {
      List<String> locations = new ArrayList<>();
      for (JsonNode locationNode : locationsNode) {
        String location = locationNode.asText("");
        if (!location.isEmpty()) {
          locations.add(location);
        }
      }

      // Process each location
      if (!locations.isEmpty()) {
        // Process primary location
        String primaryLocation = locations.get(0);
        locationService.processAssignmentLocation(assignment, primaryLocation, source.getName());

        // Process any additional locations
        for (int i = 1; i < locations.size(); i++) {
          locationService.processAssignmentLocation(assignment, locations.get(i), source.getName());
        }
      }
    }

    // Extract skills
    JsonNode skillsNode = jobNode.path("skills");
    if (skillsNode.isArray() && skillsNode.size() > 0) {
      Set<String> skillNames = new HashSet<>();
      for (JsonNode skillNode : skillsNode) {
        String skill = skillNode.asText("");
        if (!skill.isEmpty()) {
          skillNames.add(skill);
        }
      }
      processSkills(assignment, skillNames);
    }

    // Extract start date
    String startDateString = jobNode.path("startDate").asText(null);
    if (startDateString != null && !startDateString.isEmpty()) {
      try {
        LocalDate startDate = LocalDate.parse(startDateString);
        assignment.setStartDate(startDate);
      } catch (Exception e) {
        logger.warn("Could not parse start date: {}", startDateString);
      }
    }

    // Extract end date as duration
    String endDateString = jobNode.path("endDate").asText(null);
    if (startDateString != null && endDateString != null) {
      try {
        LocalDate startDate = LocalDate.parse(startDateString);
        LocalDate endDate = LocalDate.parse(endDateString);
        long monthsBetween =
            Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay()).toDays() / 30;
        assignment.setDurationMonths((int) monthsBetween);
      } catch (Exception e) {
        logger.warn("Could not calculate duration: {}", e.getMessage());
      }
    }

    // Extract rate
    String rateString = jobNode.path("rate").asText(null);
    String currency = jobNode.path("rateCurrency").asText("SEK");

    if (rateString != null && !rateString.isEmpty()) {
      try {
        BigDecimal rate = new BigDecimal(rateString);
        assignment.setHourlyRateMin(rate);
        assignment.setHourlyRateMax(rate);

        // Set currency as entity
        setCurrency(assignment, currency);
      } catch (NumberFormatException e) {
        logger.warn("Could not parse rate: {}", rateString);
      }
    } else {
      // Default to SEK if no currency specified
      setCurrency(assignment, "SEK");
    }

    // Extract hours per week
    String hoursPerWeekString = jobNode.path("utilization").asText(null);
    if (hoursPerWeekString != null && !hoursPerWeekString.isEmpty()) {
      try {
        int utilization = Integer.parseInt(hoursPerWeekString);
        // Utilization is a percentage, convert to hours (assuming 40h week)
        int hoursPerWeek = (int) Math.round(utilization * 0.4);
        assignment.setHoursPerWeek(hoursPerWeek);
      } catch (NumberFormatException e) {
        logger.warn("Could not parse utilization: {}", hoursPerWeekString);
      }
    }

    return assignment;
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
