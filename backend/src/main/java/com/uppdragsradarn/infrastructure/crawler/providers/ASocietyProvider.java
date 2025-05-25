package com.uppdragsradarn.infrastructure.crawler.providers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.uppdragsradarn.application.service.LocationService;
import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.CrawlerException;
import com.uppdragsradarn.domain.model.Currency;
import com.uppdragsradarn.domain.model.Skill;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.StatusType;
import com.uppdragsradarn.domain.repository.CurrencyRepository;
import com.uppdragsradarn.domain.repository.SkillRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;

import lombok.extern.slf4j.Slf4j;

/** Content provider for A Society Group Capable of fetching via either direct HTTP or Playwright */
@Component
@Slf4j
public class ASocietyProvider extends AbstractProvider {

  private static final String SOURCE_NAME = "A Society Group";
  private static final String BASE_URL = "https://www.asocietygroup.com";
  private static final String LIST_URL = BASE_URL + "/en/uppdrag";
  private static final String DETAIL_BASE_URL = BASE_URL + "/en/uppdrag/";
  private static final String USER_AGENT =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

  // DEPRECATED: Regex patterns replaced by LLM extraction
  private static final java.util.regex.Pattern JOB_LINK_PATTERN =
      java.util.regex.Pattern.compile("/en/uppdrag/([^\"/?]+)");

  @Value("${app.crawler.asocietygroup.use-playwright:true}")
  private boolean usePlaywright;

  @Value("${app.crawler.asocietygroup.use-direct-fetcher:false}")
  private boolean useDirectFetcher;

  private final LocationService locationService;
  private final SkillRepository skillRepository;
  private final CurrencyRepository currencyRepository;
  private final StatusTypeRepository statusTypeRepository;

  public ASocietyProvider(
      LocationService locationService,
      SkillRepository skillRepository,
      CurrencyRepository currencyRepository,
      StatusTypeRepository statusTypeRepository) {
    this.locationService = locationService;
    this.skillRepository = skillRepository;
    this.currencyRepository = currencyRepository;
    this.statusTypeRepository = statusTypeRepository;
  }

  @Override
  public String getName() {
    return "A Society Group Provider";
  }

  @Override
  public boolean supports(Source source) {
    return source.getName().equalsIgnoreCase(SOURCE_NAME)
        || (source.getBaseUrl() != null && source.getBaseUrl().contains("asocietygroup.com"));
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    // Decide which method to use based on configuration
    if (useDirectFetcher) {
      logger.info("Using direct fetcher for A Society Group");
      return fetchWithDirectFetcher(source);
    } else if (usePlaywright) {
      logger.info("Using Playwright for A Society Group");
      return fetchWithPlaywright(source);
    } else {
      // Default to Playwright
      logger.info("No specific method configured, defaulting to Playwright for A Society Group");
      return fetchWithPlaywright(source);
    }
  }

  /** Fetch assignments using Playwright browser automation */
  private List<Assignment> fetchWithPlaywright(Source source) throws CrawlerException {
    List<Assignment> assignments = new ArrayList<>();
    Playwright playwright = null;
    Browser browser = null;

    try {
      // Initialize Playwright
      playwright = Playwright.create();
      browser =
          playwright
              .chromium()
              .launch(new BrowserType.LaunchOptions().setHeadless(true).setTimeout(60000));

      // Create context
      BrowserContext context =
          browser.newContext(
              new Browser.NewContextOptions().setUserAgent(USER_AGENT).setLocale("sv-SE"));

      // Create page
      Page page = context.newPage();

      // Navigate to job listings page
      logger.info("Navigating to {}", LIST_URL);
      page.navigate(LIST_URL);
      page.waitForLoadState(
          LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(30000));

      // Look for job cards or links
      List<String> jobUrls = new ArrayList<>();
      String pageContent = page.content();
      java.util.regex.Matcher matcher = JOB_LINK_PATTERN.matcher(pageContent);

      while (matcher.find()) {
        String slug = matcher.group(1);
        if (slug != null && !slug.isEmpty()) {
          String jobUrl = DETAIL_BASE_URL + slug;
          if (!jobUrls.contains(jobUrl)) {
            jobUrls.add(jobUrl);
          }
        }
      }

      logger.info("Found {} job links", jobUrls.size());

      // Process each job detail page
      for (String jobUrl : jobUrls) {
        try {
          Assignment assignment = processDetailPage(page, jobUrl, source);
          if (assignment != null) {
            assignments.add(assignment);
          }
        } catch (Exception e) {
          logger.warn("Error processing job detail page {}: {}", jobUrl, e.getMessage());
        }
      }

    } catch (Exception e) {
      logger.error("Error fetching assignments with Playwright: {}", e.getMessage(), e);
      throw new CrawlerException("Error fetching assignments with Playwright", e);
    } finally {
      if (browser != null) {
        browser.close();
      }
      if (playwright != null) {
        playwright.close();
      }
    }

    return assignments;
  }

  /** Process a job detail page and extract an assignment */
  private Assignment processDetailPage(Page page, String url, Source source) {
    try {
      logger.debug("Processing job detail page: {}", url);

      // Navigate to job detail page
      page.navigate(url);
      page.waitForLoadState(
          LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000));

      // Get page content
      String pageContent = page.content();

      // Basic data extraction - detailed extraction should use LLM service
      String title = "Job from A Society Group"; // Will be extracted by LLM
      String description = "[To be extracted by LLM]"; // Placeholder
      String location = "Sweden"; // Default location
      String workArrangement = "Hybrid"; // Default

      // Create assignment
      Assignment assignment =
          Assignment.builder()
              .title(title)
              .description(description.replaceAll("\\\\n", "\n"))
              .applicationUrl(url)
              .externalId(url.substring(url.lastIndexOf('/') + 1))
              .companyName(source.getName())
              .source(source)
              .active(true)
              .build();

      // Set default status to ACTIVE
      StatusType activeStatus = getOrCreateStatusType("ACTIVE", "ASSIGNMENT");
      assignment.setStatus(activeStatus);

      // Process location
      locationService.processAssignmentLocation(
          assignment, location != null ? location : "Sweden", source.getName());

      // Basic skills - LLM will extract proper skills
      processSkills(assignment, Set.of("Consulting", "A Society Group"));

      return assignment;

    } catch (Exception e) {
      logger.warn("Error extracting job data from {}: {}", url, e.getMessage());
      return null;
    }
  }

  /** Fetch assignments using direct HTTP requests */
  private List<Assignment> fetchWithDirectFetcher(Source source) throws CrawlerException {
    List<Assignment> assignments = new ArrayList<>();

    try {
      // Create HTTP client with appropriate timeouts
      HttpClient client =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(30))
              .followRedirects(HttpClient.Redirect.NORMAL)
              .build();

      // Fetch job listing page
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(LIST_URL))
              .header("User-Agent", USER_AGENT)
              .header("Accept", "text/html,application/xhtml+xml")
              .GET()
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new CrawlerException(
            "Failed to fetch job listings. Status code: " + response.statusCode());
      }

      // Parse HTML to extract job links
      Document doc = Jsoup.parse(response.body());
      Elements jobLinks = doc.select("a[href^='/en/uppdrag/']");

      List<String> jobUrls =
          jobLinks.stream()
              .map(link -> BASE_URL + link.attr("href"))
              .distinct()
              .collect(Collectors.toList());

      logger.info("Found {} job links", jobUrls.size());

      // Process each job detail page
      for (String jobUrl : jobUrls) {
        try {
          Assignment assignment = processDetailPageDirect(client, jobUrl, source);
          if (assignment != null) {
            assignments.add(assignment);
          }
        } catch (Exception e) {
          logger.warn("Error processing job detail page {}: {}", jobUrl, e.getMessage());
        }
      }

    } catch (Exception e) {
      logger.error("Error fetching assignments with direct fetcher: {}", e.getMessage(), e);
      throw new CrawlerException("Error fetching assignments with direct fetcher", e);
    }

    return assignments;
  }

  /** Process a job detail page using direct HTTP requests */
  private Assignment processDetailPageDirect(HttpClient client, String url, Source source)
      throws Exception {
    // Fetch job detail page
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .GET()
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new Exception("Failed to fetch job detail. Status code: " + response.statusCode());
    }

    String pageContent = response.body();

    // Basic job data - detailed extraction should use LLM service
    String title = "Job from A Society Group";
    String description = "[To be extracted by LLM]";
    String location = "Sweden";
    String workArrangement = "Hybrid";

    // Create assignment
    Assignment assignment = new Assignment();
    assignment.setTitle(title);
    assignment.setDescription(description.replaceAll("\\\\n", "\n"));
    assignment.setApplicationUrl(url);
    assignment.setExternalId(url.substring(url.lastIndexOf('/') + 1));
    assignment.setCompanyName(source.getName());
    assignment.setSource(source);
    assignment.setActive(true);

    // Set default status to ACTIVE
    StatusType activeStatus = getOrCreateStatusType("ACTIVE", "ASSIGNMENT");
    assignment.setStatus(activeStatus);

    // Process location
    locationService.processAssignmentLocation(
        assignment, location != null ? location : "Sweden", source.getName());

    // Process skills
    processSkills(assignment, Set.of("Work Mode: " + workArrangement));

    return assignment;
  }

  // DEPRECATED: Regex extraction replaced by LLM

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

  /** Sets currency on assignment */
  private void setCurrency(Assignment assignment, String currencyCode) {
    Currency currency = findOrCreateCurrency(currencyCode);
    assignment.setCurrency(currency);
  }

  /** Finds or creates currency entity */
  private Currency findOrCreateCurrency(String code) {
    try {
      return currencyRepository
          .findByCode(code)
          .orElseGet(
              () -> {
                try {
                  Currency newCurrency =
                      Currency.builder()
                          .code(code)
                          .name(getCurrencyName(code))
                          .symbol(getCurrencySymbol(code))
                          .build();
                  return currencyRepository.save(newCurrency);
                } catch (Exception e) {
                  // If save fails (likely due to unique constraint), try to find it again
                  return currencyRepository
                      .findByCode(code)
                      .orElseThrow(
                          () ->
                              new RuntimeException(
                                  "Failed to create or find currency: " + code, e));
                }
              });
    } catch (Exception e) {
      logger.warn("Error in findOrCreateCurrency for code {}: {}", code, e.getMessage());
      // Last attempt - try to get by code one more time
      return currencyRepository
          .findByCode(code)
          .orElseThrow(
              () ->
                  new RuntimeException(
                      "Failed to create or find currency after retrying: " + code, e));
    }
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
                  StatusType newStatus =
                      StatusType.builder().name(name).entityType(entityType).build();
                  return statusTypeRepository.save(newStatus);
                } catch (Exception e) {
                  // If save fails (likely due to unique constraint), try to find it again
                  return statusTypeRepository
                      .findByNameAndEntityType(name, entityType)
                      .orElseThrow(
                          () ->
                              new RuntimeException(
                                  "Failed to create or find status type: "
                                      + name
                                      + ":"
                                      + entityType,
                                  e));
                }
              });
    } catch (Exception e) {
      logger.warn("Error in getOrCreateStatusType for {}:{}: {}", name, entityType, e.getMessage());
      // Last attempt - try to get by name and type one more time
      return statusTypeRepository
          .findByNameAndEntityType(name, entityType)
          .orElseThrow(
              () ->
                  new RuntimeException(
                      "Failed to create or find status type after retrying: "
                          + name
                          + ":"
                          + entityType,
                      e));
    }
  }
}
