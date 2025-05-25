package com.uppdragsradarn.infrastructure.crawler.providers;

import java.time.LocalDate;
import java.time.Year;
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
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.repository.CurrencyRepository;
import com.uppdragsradarn.domain.repository.SkillRepository;
import com.uppdragsradarn.domain.repository.SourceTypeRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Provider for Konsultfabriken job listings. Simple PHP-based job board with straightforward HTML
 * structure.
 */
@Component
@Slf4j
public class KonsultfabrikenProvider extends AbstractHttpProvider {

  private static final String LISTINGS_URL = "https://www.konsultfabriken.se/all-assignments.php";
  private static final String JOB_DETAIL_URL = "https://www.konsultfabriken.se/job.php?id=";
  private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{2})/(\\d{2})");
  private static final Pattern JOB_ID_PATTERN = Pattern.compile("job\\.php\\?id=(\\d+)");

  private final LocationService locationService;
  private final SkillRepository skillRepository;
  private final CurrencyRepository currencyRepository;
  private final SourceTypeRepository sourceTypeRepository;
  private final StatusTypeRepository statusTypeRepository;

  public KonsultfabrikenProvider(
      LocationService locationService,
      SkillRepository skillRepository,
      CurrencyRepository currencyRepository,
      SourceTypeRepository sourceTypeRepository,
      StatusTypeRepository statusTypeRepository) {
    super();
    this.locationService = locationService;
    this.skillRepository = skillRepository;
    this.currencyRepository = currencyRepository;
    this.sourceTypeRepository = sourceTypeRepository;
    this.statusTypeRepository = statusTypeRepository;
  }

  @Override
  public String getName() {
    return "Konsultfabriken Provider";
  }

  @Override
  public boolean supports(Source source) {
    return source.getName().equalsIgnoreCase("Konsultfabriken")
        || (source.getBaseUrl() != null && source.getBaseUrl().contains("konsultfabriken.se"));
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    logger.info("Fetching assignments from Konsultfabriken: {}", LISTINGS_URL);

    try {
      Document doc = fetchAndParseDocument(LISTINGS_URL);
      List<Assignment> assignments = new ArrayList<>();

      // Find all links to job.php
      Elements jobLinks = doc.select("a[href*='job.php?id=']");

      logger.debug("Found {} job links", jobLinks.size());

      for (Element jobLink : jobLinks) {
        try {
          Assignment assignment = extractAssignmentFromLink(jobLink, source);
          if (assignment != null) {
            // Fetch additional details from job page
            enrichFromDetailPage(assignment);
            assignments.add(assignment);
          }
        } catch (Exception e) {
          logger.warn("Error extracting assignment: {}", e.getMessage());
        }
      }

      logger.info("Extracted {} assignments from Konsultfabriken", assignments.size());
      return assignments;

    } catch (Exception e) {
      throw new CrawlerException("Failed to fetch Konsultfabriken listings", e);
    }
  }

  private Assignment extractAssignmentFromLink(Element jobLink, Source source) {
    Assignment assignment = new Assignment();
    assignment.setSource(source);
    assignment.setActive(true);
    assignment.setStatus(getOrCreateStatusType("ACTIVE", "ASSIGNMENT"));

    // Extract title from link text
    String title = jobLink.text().trim();
    if (title.isEmpty()) {
      return null;
    }
    assignment.setTitle(title);

    // Extract job ID from URL
    String href = jobLink.attr("href");
    Matcher idMatcher = JOB_ID_PATTERN.matcher(href);
    if (idMatcher.find()) {
      String jobId = idMatcher.group(1);
      assignment.setExternalId(jobId);
      assignment.setApplicationUrl(JOB_DETAIL_URL + jobId);
    } else {
      return null;
    }

    // Extract date from preceding text
    LocalDate publishDate = extractDateFromPrecedingText(jobLink);
    if (publishDate != null) {
      // For now, we'll just log this - could be used for other date fields based on business logic
      logger.debug("Assignment {} was published on {}", title, publishDate);
    }

    // Set company
    assignment.setCompanyName("Konsultfabriken");

    // Extract location from title if present
    String location = extractLocationFromTitle(title);
    locationService.processAssignmentLocation(assignment, location, source.getName());

    return assignment;
  }

  private void enrichFromDetailPage(Assignment assignment) {
    try {
      Document doc = fetchAndParseDocument(assignment.getApplicationUrl());

      // Extract full content
      String content = extractJobContent(doc);
      if (content != null && !content.isEmpty()) {
        assignment.setDescription(content);

        // Extract additional location info
        String detailedLocation = extractLocationFromContent(content);
        if (!detailedLocation.equals("Sweden")) {
          locationService.processAssignmentLocation(
              assignment, detailedLocation, assignment.getSource().getName());
        }

        // Extract skills
        Set<String> skills = extractSkillsFromContent(content);
        for (String skillName : skills) {
          try {
            assignment.addSkill(findOrCreateSkill(skillName));
          } catch (Exception e) {
            logger.debug("Could not add skill: {}", skillName);
          }
        }

        // Extract rate if present
        extractAndSetRate(assignment, content);
      }

    } catch (Exception e) {
      logger.debug("Could not enrich assignment from detail page: {}", e.getMessage());
    }
  }

  private LocalDate extractDateFromPrecedingText(Element jobLink) {
    // Look for date pattern in preceding text node
    Element parent = jobLink.parent();
    if (parent != null) {
      String text = parent.text();
      Matcher matcher = DATE_PATTERN.matcher(text);
      if (matcher.find()) {
        try {
          int day = Integer.parseInt(matcher.group(1));
          int month = Integer.parseInt(matcher.group(2));
          int year = Year.now().getValue();

          // Adjust year if date is in the future
          LocalDate date = LocalDate.of(year, month, day);
          if (date.isAfter(LocalDate.now())) {
            date = date.minusYears(1);
          }

          return date;
        } catch (Exception e) {
          logger.debug("Could not parse date: {}/{}", matcher.group(1), matcher.group(2));
        }
      }
    }

    return null;
  }

  private String extractLocationFromTitle(String title) {
    String lowercaseTitle = title.toLowerCase();

    // Check for explicit location mentions
    if (lowercaseTitle.contains("stockholm")) return "Stockholm";
    if (lowercaseTitle.contains("göteborg") || lowercaseTitle.contains("gothenburg"))
      return "Göteborg";
    if (lowercaseTitle.contains("malmö")) return "Malmö";
    if (lowercaseTitle.contains("uppsala")) return "Uppsala";
    if (lowercaseTitle.contains("linköping")) return "Linköping";

    // Check for remote work
    if (lowercaseTitle.contains("remote") || lowercaseTitle.contains("distans")) {
      return "Remote";
    }

    return "Sweden";
  }

  private String extractJobContent(Document doc) {
    // Try different selectors for job content
    Element contentElement = doc.selectFirst("body");

    if (contentElement != null) {
      // Clone to avoid modifying original
      Element clone = contentElement.clone();

      // Remove navigation and header elements
      clone.select("nav, header, footer, script, style").remove();

      // Get text content
      String text = clone.text();

      // Clean up
      text = text.replaceAll("\\s+", " ").trim();

      // Remove common header/footer text
      text = text.replaceAll("All assignments.*?\\|", "");
      text = text.replaceAll("\\|.*?Close", "");

      // Limit length
      if (text.length() > 5000) {
        text = text.substring(0, 5000) + "...";
      }

      return text;
    }

    return "";
  }

  private String extractLocationFromContent(String content) {
    String lowercaseContent = content.toLowerCase();

    // Swedish cities
    Map<String, String> cities = new HashMap<>();
    cities.put("stockholm", "Stockholm");
    cities.put("göteborg", "Göteborg");
    cities.put("gothenburg", "Göteborg");
    cities.put("malmö", "Malmö");
    cities.put("uppsala", "Uppsala");
    cities.put("västerås", "Västerås");
    cities.put("örebro", "Örebro");
    cities.put("linköping", "Linköping");
    cities.put("helsingborg", "Helsingborg");
    cities.put("jönköping", "Jönköping");
    cities.put("norrköping", "Norrköping");
    cities.put("lund", "Lund");
    cities.put("umeå", "Umeå");

    for (Map.Entry<String, String> entry : cities.entrySet()) {
      if (lowercaseContent.contains(entry.getKey())) {
        return entry.getValue();
      }
    }

    // Check for remote indicators
    if (lowercaseContent.contains("remote work")
        || lowercaseContent.contains("distansarbete")
        || lowercaseContent.contains("work from home")
        || lowercaseContent.contains("hemarbete")) {
      return "Remote";
    }

    return "Sweden";
  }

  private Set<String> extractSkillsFromContent(String content) {
    Set<String> skills = new HashSet<>();
    String lowercaseContent = content.toLowerCase();

    // Technical skills
    String[] techSkills = {
      "java",
      "spring",
      "spring boot",
      "microservices",
      "rest",
      "api",
      "javascript",
      "typescript",
      "react",
      "angular",
      "vue",
      "nodejs",
      "python",
      "django",
      "flask",
      "fastapi",
      "c#",
      ".net",
      "asp.net",
      "azure",
      "aws",
      "gcp",
      "cloud",
      "docker",
      "kubernetes",
      "devops",
      "ci/cd",
      "jenkins",
      "gitlab",
      "sql",
      "mysql",
      "postgresql",
      "oracle",
      "mongodb",
      "redis",
      "git",
      "agile",
      "scrum",
      "kanban",
      "jira",
      "frontend",
      "backend",
      "fullstack",
      "mobile",
      "ios",
      "android",
      "linux",
      "windows",
      "security",
      "testing",
      "automation"
    };

    for (String skill : techSkills) {
      if (lowercaseContent.contains(skill)) {
        skills.add(skill.toUpperCase().replace(" ", "_").replace(".", ""));
      }
    }

    return skills;
  }

  private void extractAndSetRate(Assignment assignment, String content) {
    // Look for rate patterns
    Pattern ratePattern =
        Pattern.compile(
            "(\\d{3,4})\\s*(-\\s*(\\d{3,4}))?\\s*(kr|SEK|per timme|/h|/tim)",
            Pattern.CASE_INSENSITIVE);
    Matcher matcher = ratePattern.matcher(content);

    if (matcher.find()) {
      try {
        String minRate = matcher.group(1);
        String maxRate = matcher.group(3);

        assignment.setHourlyRateMin(new java.math.BigDecimal(minRate));

        if (maxRate != null) {
          assignment.setHourlyRateMax(new java.math.BigDecimal(maxRate));
        } else {
          assignment.setHourlyRateMax(assignment.getHourlyRateMin());
        }

        // Set currency to SEK
        assignment.setCurrency(findOrCreateCurrency("SEK"));

      } catch (Exception e) {
        logger.debug("Could not parse rate from content");
      }
    }
  }

  private com.uppdragsradarn.domain.model.Currency findOrCreateCurrency(String code) {
    return currencyRepository
        .findByCode(code)
        .orElseGet(
            () -> {
              var currency =
                  com.uppdragsradarn.domain.model.Currency.builder()
                      .code(code)
                      .name("Swedish Krona")
                      .symbol("kr")
                      .build();
              return currencyRepository.save(currency);
            });
  }

  private com.uppdragsradarn.domain.model.StatusType getOrCreateStatusType(
      String name, String entityType) {
    return statusTypeRepository
        .findByNameAndEntityType(name, entityType)
        .orElseGet(
            () -> {
              var newStatus =
                  com.uppdragsradarn.domain.model.StatusType.builder()
                      .name(name)
                      .entityType(entityType)
                      .build();
              return statusTypeRepository.save(newStatus);
            });
  }

  private com.uppdragsradarn.domain.model.Skill findOrCreateSkill(String skillName) {
    return skillRepository
        .findByNameIgnoreCase(skillName)
        .orElseGet(
            () -> {
              var newSkill =
                  com.uppdragsradarn.domain.model.Skill.builder().name(skillName).build();
              return skillRepository.save(newSkill);
            });
  }
}
