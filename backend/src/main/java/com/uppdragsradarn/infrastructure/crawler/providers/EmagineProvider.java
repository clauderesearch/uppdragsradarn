package com.uppdragsradarn.infrastructure.crawler.providers;

// Removed imports for deprecated extraction methods
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
// Removed regex and date parsing imports - now using LLM extraction

import org.apache.hc.client5.http.classic.methods.HttpGet;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
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

import lombok.extern.slf4j.Slf4j;

/** Content provider for Emagine jobs */
@Component
@Slf4j
public class EmagineProvider extends AbstractProvider {

  private static final String AJAX_ENDPOINT =
      "/wp-content/themes/emagine-theme/ajax/api-country.php";
  private static final String PUBLIC_URL_PATH = "/consultants/freelance-jobs/";
  private static final String DEFAULT_AJAX_DATA =
      "action=get_jobs_by_country&dats=%7B%22sorting%22%3A%22Id+DESC%22%2C%22skipCount%22%3A0%2C%22maxResultCount%22%3A1000%2C%22filter%22%3A%22%22%2C%22primaryCategoryIds%22%3A%5B%5D%2C%22tenantIds%22%3A%5B2%5D%2C%22isFullyRemote%22%3Anull%2C%22countryId%22%3Anull%2C%22geoCoordinate%22%3A%7B%22latitude%22%3A0%2C%22longitude%22%3A0%7D%2C%22maxDistanceToWorkInKm%22%3A10000%2C%22permittedSortingFields%22%3A%5B%5D%2C%22language%22%3A%22sv%22%2C%22filters%22%3A%5B%5D%7D";

  // Deprecated regex patterns removed - now using LLM extraction

  private final int timeoutSeconds;
  private final String userAgent;
  private final int maxAssignments;
  private final String baseUrl;

  private final LocationService locationService;
  private final SkillRepository skillRepository;
  private final CurrencyRepository currencyRepository;
  private final SourceTypeRepository sourceTypeRepository;
  private final StatusTypeRepository statusTypeRepository;

  // Primary constructor
  public EmagineProvider(
      @Value("${app.crawler.timeout:30}") int timeoutSeconds,
      @Value(
              "${app.crawler.user-agent:Mozilla/5.0 (compatible; UppdragsRadarn/1.0; +https://uppdragsradarn.se/bot)}")
          String userAgent,
      @Value("${app.crawler.max-assignments:200}") int maxAssignments,
      @Value("${app.crawler.emagine.url:https://emagine-consulting.se}") String baseUrl,
      LocationService locationService,
      SkillRepository skillRepository,
      CurrencyRepository currencyRepository,
      SourceTypeRepository sourceTypeRepository,
      StatusTypeRepository statusTypeRepository) {
    this.timeoutSeconds = timeoutSeconds;
    this.userAgent = userAgent;
    this.maxAssignments = maxAssignments;
    this.baseUrl = baseUrl;
    this.locationService = locationService;
    this.skillRepository = skillRepository;
    this.currencyRepository = currencyRepository;
    this.sourceTypeRepository = sourceTypeRepository;
    this.statusTypeRepository = statusTypeRepository;
  }

  @Override
  public String getName() {
    return "Emagine Provider";
  }

  @Override
  public boolean supports(Source source) {
    return source.getName().equalsIgnoreCase("Emagine Consulting")
        || (source.getSourceType() != null && "EMAGINE".equals(source.getSourceType().getName()))
        || (source.getBaseUrl() != null && source.getBaseUrl().contains("emagine"));
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    logger.info("Fetching assignments from Emagine: {}", baseUrl);

    try (CloseableHttpClient httpClient = createHttpClient()) {
      // Construct AJAX URL
      String ajaxUrl = baseUrl + AJAX_ENDPOINT;
      logger.debug("Making POST request to AJAX endpoint: {}", ajaxUrl);

      // Create and configure POST request
      HttpPost httpPost = new HttpPost(ajaxUrl);
      httpPost.setHeader("User-Agent", userAgent);
      httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
      httpPost.setHeader("X-Requested-With", "XMLHttpRequest");
      httpPost.setHeader("Origin", baseUrl);
      httpPost.setHeader("Referer", baseUrl + PUBLIC_URL_PATH);

      // Set POST data
      httpPost.setEntity(
          new StringEntity(DEFAULT_AJAX_DATA, ContentType.APPLICATION_FORM_URLENCODED));

      // Execute request
      try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
        int statusCode = response.getCode();
        if (statusCode != 200) {
          logger.warn("Failed to fetch assignments: Status code: {}", statusCode);
          throw new CrawlerException("Failed to fetch assignments. Status code: " + statusCode);
        }

        // Parse response
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          String html = EntityUtils.toString(entity);
          List<Assignment> assignments = parseJobListings(html, source);
          logger.info("Successfully fetched {} assignments from Emagine", assignments.size());
          return assignments;
        } else {
          throw new CrawlerException("Empty response from Emagine");
        }
      }
    } catch (Exception e) {
      logger.error("Error fetching assignments from Emagine: {}", e.getMessage(), e);
      throw new CrawlerException("Error fetching assignments: " + e.getMessage(), e);
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

  /** Parses the HTML response containing job listings */
  private List<Assignment> parseJobListings(String html, Source source) throws CrawlerException {
    try {
      List<Assignment> assignments = new ArrayList<>();
      Document doc = Jsoup.parse(html);
      Elements jobElements = doc.select(".row.consultants-list");

      if (jobElements.isEmpty()) {
        logger.warn("No job elements found in HTML response");
        return assignments;
      }

      for (Element jobElement : jobElements) {
        try {
          Assignment assignment = createAssignmentFromElement(jobElement, source);
          if (assignment != null) {
            assignments.add(assignment);

            // Limit the number of assignments to prevent overwhelming the system
            if (assignments.size() >= maxAssignments) {
              logger.info("Reached maximum assignment limit ({}), stopping", maxAssignments);
              break;
            }
          }
        } catch (Exception e) {
          logger.warn("Error parsing job element: {}", e.getMessage());
        }
      }

      return assignments;
    } catch (Exception e) {
      logger.error("Error parsing job listings: {}", e.getMessage(), e);
      throw new CrawlerException("Error parsing job listings: " + e.getMessage(), e);
    }
  }

  /** Creates an Assignment object from a job listing element */
  private Assignment createAssignmentFromElement(Element jobElement, Source source) {
    Element titleElement = jobElement.selectFirst(".consultants-list-title > a");
    if (titleElement == null) {
      logger.debug("Skipping job element without title");
      return null;
    }

    String title = titleElement.text().trim();
    String linkHref = titleElement.attr("href");
    String applicationUrl = baseUrl + linkHref;

    // Extract external ID from URL
    String externalId = extractExternalIdFromUrl(linkHref);
    if (externalId == null || externalId.isEmpty()) {
      externalId = String.valueOf(applicationUrl.hashCode());
    }

    // Get company name from tagline or source name
    Element taglineElement = jobElement.selectFirst(".consultants-list-tagline");
    String companyName = taglineElement != null ? taglineElement.text().trim() : source.getName();

    // Create assignment object
    Assignment assignment = new Assignment();
    assignment.setTitle(title);
    assignment.setApplicationUrl(applicationUrl);
    assignment.setExternalId(externalId);
    assignment.setCompanyName(companyName);
    assignment.setSource(source);
    assignment.setActive(true);

    // Set default status to ACTIVE
    StatusType activeStatus = getOrCreateStatusType("ACTIVE", "ASSIGNMENT");
    assignment.setStatus(activeStatus);

    // Default currency to SEK
    setCurrency(assignment, "SEK");

    // Basic description will be extracted by LLM if this provider is migrated
    assignment.setDescription("[To be extracted by LLM]");

    // Extract category as skills
    Element categoryElement = jobElement.selectFirst(".consultants-list-category");
    if (categoryElement != null) {
      String category = categoryElement.text().trim();
      if (!category.isEmpty()) {
        Set<String> skills = new HashSet<>();
        skills.add(category);
        processSkills(assignment, skills);
      }
    }

    // Extract location from element
    Element locationElement = jobElement.selectFirst(".consultants-list-customer");
    if (locationElement != null) {
      String location = locationElement.text().trim();
      if (!location.isEmpty()) {
        locationService.processAssignmentLocation(assignment, location, source.getName());
      }
    }

    return assignment;
  }

  /** Extracts job ID from the URL path */
  private String extractExternalIdFromUrl(String url) {
    // Extract last path segment as ID
    String[] pathParts = url.split("/");
    if (pathParts.length > 0) {
      String lastPart = pathParts[pathParts.length - 1];
      // Remove query string if present
      if (lastPart.contains("?")) {
        lastPart = lastPart.substring(0, lastPart.indexOf("?"));
      }
      return lastPart;
    }
    return null;
  }

  // DEPRECATED: Detail page extraction replaced by LLM-based extraction

  // DEPRECATED: HTML extraction methods replaced by LLM-based extraction

  // DEPRECATED: Complex regex-based field extraction replaced by LLM

  // DEPRECATED: Date parsing replaced by LLM extraction

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
                  return skillRepository.findByNameIgnoreCase(skillName)
                      .orElseThrow(() -> new RuntimeException(
                          "Failed to create or find skill: " + skillName, e));
                }
              });
    } catch (Exception e) {
      log.warn("Error in findOrCreateSkill for skill {}: {}", skillName, e.getMessage());
      // Last attempt - try to get by name one more time
      return skillRepository.findByNameIgnoreCase(skillName)
          .orElseThrow(() -> new RuntimeException(
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
    try {
      return statusTypeRepository
          .findByNameAndEntityType(name, entityType)
          .orElseGet(
              () -> {
                try {
                  StatusType newStatus = StatusType.builder().name(name).entityType(entityType).build();
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
