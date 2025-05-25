package com.uppdragsradarn.infrastructure.crawler.providers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

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
import com.uppdragsradarn.infrastructure.crawler.config.CrawlerConfiguration;
import com.uppdragsradarn.infrastructure.crawler.config.CrawlerConfigurationService;

import lombok.extern.slf4j.Slf4j;

/**
 * A configurable provider for standard job sites that follow common patterns. Uses CSS selectors
 * configured in the source parameters to extract job data.
 */
@Component
@Slf4j
public class StandardJobSiteProvider extends AbstractHttpProvider {

  // Configuration keys for source parameters
  private static final String CONFIG_LIST_URL = "listUrl";
  private static final String CONFIG_JOB_SELECTOR = "jobSelector";
  private static final String CONFIG_TITLE_SELECTOR = "titleSelector";
  private static final String CONFIG_LINK_SELECTOR = "linkSelector";
  private static final String CONFIG_COMPANY_SELECTOR = "companySelector";
  private static final String CONFIG_LOCATION_SELECTOR = "locationSelector";
  private static final String CONFIG_DESCRIPTION_SELECTOR = "descriptionSelector";
  private static final String CONFIG_DEADLINE_SELECTOR = "deadlineSelector";
  private static final String CONFIG_RATE_SELECTOR = "rateSelector";
  private static final String CONFIG_SKILLS_SELECTOR = "skillsSelector";
  private static final String CONFIG_PAGINATION_SELECTOR = "paginationSelector";
  private static final String CONFIG_MAX_PAGES = "maxPages";
  private static final String CONFIG_DETAIL_REQUIRED = "detailRequired";

  // Patterns for parsing data
  private static final Pattern RATE_PATTERN =
      Pattern.compile("(\\d+)(?:[\\s-]*(\\d+))?\\s*([A-Z]{3})?");
  private static final Pattern DATE_PATTERNS =
      Pattern.compile(
          "(\\d{4}-\\d{2}-\\d{2})|"
              + // ISO date
              "(\\d{1,2}/\\d{1,2}/\\d{4})|"
              + // US date
              "(\\d{1,2}\\.\\d{1,2}\\.\\d{4})" // European date
          );

  private final CrawlerConfigurationService configurationService;
  private final LocationService locationService;
  private final SkillRepository skillRepository;
  private final CurrencyRepository currencyRepository;
  private final SourceTypeRepository sourceTypeRepository;
  private final StatusTypeRepository statusTypeRepository;

  public StandardJobSiteProvider(
      CrawlerConfigurationService configurationService,
      LocationService locationService,
      SkillRepository skillRepository,
      CurrencyRepository currencyRepository,
      SourceTypeRepository sourceTypeRepository,
      StatusTypeRepository statusTypeRepository) {
    super(); // Explicitly call parent constructor
    this.configurationService = configurationService;
    this.locationService = locationService;
    this.skillRepository = skillRepository;
    this.currencyRepository = currencyRepository;
    this.sourceTypeRepository = sourceTypeRepository;
    this.statusTypeRepository = statusTypeRepository;
  }

  @Override
  public String getName() {
    return "Standard Job Site Provider";
  }

  @Override
  public boolean supports(Source source) {
    // This provider supports any source configured with WEB_SCRAPER type
    // and has the required configuration parameters
    return source.getSourceType() != null
        && "WEB_SCRAPER".equals(source.getSourceType().getName())
        && source.getParameters() != null
        && source.getParameters().containsKey(CONFIG_LIST_URL)
        && source.getParameters().containsKey(CONFIG_JOB_SELECTOR);
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    // Get configuration for this source
    CrawlerConfiguration crawlerConfig = configurationService.getConfiguration(source);
    Map<String, String> config = crawlerConfig.getSelectors();
    if (config == null && source.getParameters() != null) {
      // Convert Map<String, Object> to Map<String, String>
      config =
          source.getParameters().entrySet().stream()
              .collect(
                  java.util.stream.Collectors.toMap(
                      Map.Entry::getKey,
                      e -> e.getValue() != null ? e.getValue().toString() : null));
    }
    if (config == null) {
      throw new CrawlerException("No configuration found for source: " + source.getName());
    }

    String listUrl = config.get(CONFIG_LIST_URL);
    if (listUrl == null) {
      throw new CrawlerException("No list URL configured for source: " + source.getName());
    }

    List<Assignment> assignments = new ArrayList<>();
    int maxPages = crawlerConfig.getMaxPages() != null ? crawlerConfig.getMaxPages() : 1;

    // Process each page
    for (int page = 1; page <= maxPages; page++) {
      String pageUrl = buildPageUrl(listUrl, page);
      logger.info("Fetching page {} from: {}", page, pageUrl);

      try {
        Document doc = fetchAndParseDocument(pageUrl);
        List<Assignment> pageAssignments =
            extractAssignmentsFromPage(doc, source, config, crawlerConfig);
        assignments.addAll(pageAssignments);

        // Check if there's a next page
        if (!hasNextPage(doc, config)) {
          logger.info("No more pages found, stopping at page {}", page);
          break;
        }

      } catch (Exception e) {
        logger.error("Error processing page {}: {}", page, e.getMessage());
        if (page == 1) {
          // If first page fails, throw exception
          throw new CrawlerException("Failed to fetch first page", e);
        }
        // Otherwise, continue with what we have
        break;
      }
    }

    logger.info("Extracted {} assignments from {}", assignments.size(), source.getName());
    return assignments;
  }

  /** Extracts assignments from a single page. */
  private List<Assignment> extractAssignmentsFromPage(
      Document doc, Source source, Map<String, String> config, CrawlerConfiguration crawlerConfig) {
    String jobSelector = config.get(CONFIG_JOB_SELECTOR);
    Elements jobElements = doc.select(jobSelector);

    logger.debug("Found {} job elements using selector: {}", jobElements.size(), jobSelector);

    List<Assignment> assignments = new ArrayList<>();
    boolean detailRequired =
        crawlerConfig.getDetailRequired() != null ? crawlerConfig.getDetailRequired() : false;

    for (Element jobElement : jobElements) {
      try {
        Assignment assignment = extractAssignmentFromElement(jobElement, source, config);

        // If detail page is required, fetch additional information
        if (detailRequired && assignment.getApplicationUrl() != null) {
          enrichAssignmentFromDetail(assignment, config);
        }

        assignments.add(assignment);
      } catch (Exception e) {
        logger.warn("Error extracting assignment: {}", e.getMessage());
      }
    }

    return assignments;
  }

  /** Extracts assignment data from a job element. */
  private Assignment extractAssignmentFromElement(
      Element element, Source source, Map<String, String> config) {
    Assignment assignment = new Assignment();
    assignment.setSource(source);
    assignment.setActive(true);

    // Set default status to ACTIVE
    StatusType activeStatus = getOrCreateStatusType("ACTIVE", "ASSIGNMENT");
    assignment.setStatus(activeStatus);

    // Extract title (required)
    String title = extractText(element, config.get(CONFIG_TITLE_SELECTOR));
    if (title == null || title.isEmpty()) {
      throw new IllegalArgumentException("No title found for job");
    }
    assignment.setTitle(title);

    // Extract link
    String link = extractLink(element, config.get(CONFIG_LINK_SELECTOR));
    if (link != null) {
      assignment.setApplicationUrl(normalizeUrl(link, source.getBaseUrl()));
      assignment.setExternalId(generateExternalId(link));
    } else {
      // Generate ID from title if no link
      assignment.setExternalId(generateExternalId(title));
    }

    // Extract company
    String company = extractText(element, config.get(CONFIG_COMPANY_SELECTOR));
    assignment.setCompanyName(company != null ? company : source.getName());

    // Extract location and process it
    String locationText = extractText(element, config.get(CONFIG_LOCATION_SELECTOR));
    String defaultLocation = "Sweden";
    String locationToProcess = locationText != null ? locationText : defaultLocation;

    // Process location using LocationService
    locationService.processAssignmentLocation(assignment, locationToProcess, source.getName());

    // Extract description (if available on listing)
    String description = extractText(element, config.get(CONFIG_DESCRIPTION_SELECTOR));
    assignment.setDescription(description != null ? description : "");

    // Extract deadline
    String deadline = extractText(element, config.get(CONFIG_DEADLINE_SELECTOR));
    if (deadline != null) {
      LocalDate deadlineDate = parseDate(deadline);
      if (deadlineDate != null) {
        assignment.setApplicationDeadline(deadlineDate);
      }
    }

    // Extract rate
    String rate = extractText(element, config.get(CONFIG_RATE_SELECTOR));
    if (rate != null) {
      parseAndSetRate(assignment, rate);
    }

    // Extract skills
    String skillsSelector = config.get(CONFIG_SKILLS_SELECTOR);
    if (skillsSelector != null) {
      Set<String> skillNames = extractSkillNames(element, skillsSelector);
      procesSkills(assignment, skillNames);
    }

    return assignment;
  }

  /** Enriches assignment with data from detail page. */
  private void enrichAssignmentFromDetail(Assignment assignment, Map<String, String> config) {
    try {
      Document detailDoc = fetchAndParseDocument(assignment.getApplicationUrl());

      // Update description if better one is available on detail page
      String detailDescSelector = config.get(CONFIG_DESCRIPTION_SELECTOR + ".detail");
      if (detailDescSelector != null) {
        String rawDescription = extractText(detailDoc, detailDescSelector);
        if (rawDescription != null && !rawDescription.isEmpty()) {
          // Use LLM to clean and format the description consistently
          String cleanedDescription = cleanAndFormatDescription(rawDescription, detailDoc);
          assignment.setDescription(cleanedDescription);
        }
      }

      // Extract additional fields from detail page
      String rateSelector = config.get(CONFIG_RATE_SELECTOR + ".detail");
      if (rateSelector != null) {
        String rate = extractText(detailDoc, rateSelector);
        if (rate != null) {
          parseAndSetRate(assignment, rate);
        }
      }

      // Extract skills from detail page
      String skillsSelector = config.get(CONFIG_SKILLS_SELECTOR + ".detail");
      if (skillsSelector != null) {
        Set<String> skillNames = extractSkillNames(detailDoc, skillsSelector);
        procesSkills(assignment, skillNames);
      }

    } catch (Exception e) {
      logger.warn("Error fetching detail page for {}: {}", assignment.getTitle(), e.getMessage());
    }
  }

  /** Extracts text from element using selector. */
  private String extractText(Element element, String selector) {
    if (selector == null) return null;
    Element selected = element.selectFirst(selector);
    return selected != null ? selected.text().trim() : null;
  }

  /** Extracts link from element using selector. */
  private String extractLink(Element element, String selector) {
    if (selector == null) return null;
    Element selected = element.selectFirst(selector);
    if (selected != null) {
      String href = selected.attr("href");
      if (href != null && !href.isEmpty()) {
        return href;
      }
    }
    return null;
  }

  /** Extracts skill names from element. */
  private Set<String> extractSkillNames(Element element, String selector) {
    Set<String> skills = new HashSet<>();
    Elements skillElements = element.select(selector);
    for (Element skillElement : skillElements) {
      String skill = skillElement.text().trim();
      if (!skill.isEmpty()) {
        skills.add(skill);
      }
    }
    return skills;
  }

  /** Processes skills and adds them to assignment */
  private void procesSkills(Assignment assignment, Set<String> skillNames) {
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
      log.warn("Error in findOrCreateSkill for skill {}: {}", skillName, e.getMessage());
      // Last attempt - try to get by name one more time
      return skillRepository
          .findByNameIgnoreCase(skillName)
          .orElseThrow(
              () ->
                  new RuntimeException(
                      "Failed to create or find skill after retrying: " + skillName, e));
    }
  }

  /** Parses date from various formats. */
  private LocalDate parseDate(String dateStr) {
    Matcher matcher = DATE_PATTERNS.matcher(dateStr);

    while (matcher.find()) {
      for (int i = 1; i <= matcher.groupCount(); i++) {
        String group = matcher.group(i);
        if (group != null) {
          try {
            // Try different date formats
            if (group.contains("-")) {
              return LocalDate.parse(group, DateTimeFormatter.ISO_DATE);
            } else if (group.contains("/")) {
              return LocalDate.parse(group, DateTimeFormatter.ofPattern("M/d/yyyy"));
            } else if (group.contains(".")) {
              return LocalDate.parse(group, DateTimeFormatter.ofPattern("d.M.yyyy"));
            }
          } catch (DateTimeParseException e) {
            // Try next format
          }
        }
      }
    }

    return null;
  }

  /** Parses rate information and sets it on assignment. */
  private void parseAndSetRate(Assignment assignment, String rateStr) {
    Matcher matcher = RATE_PATTERN.matcher(rateStr);
    if (matcher.find()) {
      try {
        String minStr = matcher.group(1);
        String maxStr = matcher.group(2);
        String currencyCode = matcher.group(3);

        BigDecimal minRate = new BigDecimal(minStr);
        assignment.setHourlyRateMin(minRate);

        if (maxStr != null) {
          BigDecimal maxRate = new BigDecimal(maxStr);
          assignment.setHourlyRateMax(maxRate);
        } else {
          assignment.setHourlyRateMax(minRate);
        }

        // Handle currency as entity
        if (currencyCode != null) {
          setCurrency(assignment, currencyCode);
        } else if (rateStr.contains("SEK") || rateStr.contains("kr")) {
          setCurrency(assignment, "SEK");
        } else {
          // Default to SEK
          setCurrency(assignment, "SEK");
        }

      } catch (NumberFormatException e) {
        logger.warn("Could not parse rate: {}", rateStr);
      }
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

  /** Normalizes relative URLs to absolute. */
  private String normalizeUrl(String url, String baseUrl) {
    if (url.startsWith("http://") || url.startsWith("https://")) {
      return url;
    }

    if (url.startsWith("/")) {
      // Absolute path
      return baseUrl + url;
    } else {
      // Relative path
      return baseUrl + "/" + url;
    }
  }

  /** Generates external ID from URL or title. */
  private String generateExternalId(String input) {
    // Extract ID from URL if possible
    Pattern idPattern = Pattern.compile("/([a-zA-Z0-9\\-_]+)/?$");
    Matcher matcher = idPattern.matcher(input);
    if (matcher.find()) {
      return matcher.group(1);
    }

    // Otherwise, generate from hash
    return String.valueOf(input.hashCode());
  }

  /** Builds URL for specific page number. */
  private String buildPageUrl(String baseUrl, int page) {
    if (page == 1) {
      return baseUrl;
    }

    // Common pagination patterns
    if (baseUrl.contains("?")) {
      return baseUrl + "&page=" + page;
    } else {
      return baseUrl + "?page=" + page;
    }
  }

  /** Checks if there's a next page. */
  private boolean hasNextPage(Document doc, Map<String, String> config) {
    String paginationSelector = config.get(CONFIG_PAGINATION_SELECTOR);
    if (paginationSelector == null) {
      return false;
    }

    // Look for next page link
    Element nextLink = doc.selectFirst(paginationSelector);
    return nextLink != null && !nextLink.hasClass("disabled");
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

  /**
   * Cleans and formats job description using LLM for consistent output. This method should
   * integrate with your actual LLM service.
   *
   * @param rawDescription The raw description text or HTML
   * @param document The full HTML document for context if needed
   * @return Cleaned and formatted description
   */
  private String cleanAndFormatDescription(String rawDescription, Document document) {
    // TODO: Integrate with your LLM service
    // This is a placeholder showing how to use the prompt

    // Example of how to use with an LLM service:
    // String prompt = JobDescriptionPrompts.EXTRACTION_PROMPT + rawDescription;
    // String cleanedDescription = llmService.processText(prompt);
    // return cleanedDescription;

    // For now, returning a basic cleaned version
    // In production, replace this with actual LLM integration
    return rawDescription
        .replaceAll("<[^>]+>", "") // Remove HTML tags
        .replaceAll("\\s+", " ") // Normalize whitespace
        .trim();
  }

  /** Alternative method for simple cleaning without full HTML context */
  private String simpleCleanDescription(String text) {
    // For simpler cases, use the simple cleaning prompt
    // String prompt = JobDescriptionPrompts.SIMPLE_CLEANING_PROMPT + text;
    // return llmService.processText(prompt);

    return text.trim();
  }
}
