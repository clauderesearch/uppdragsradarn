package com.uppdragsradarn.infrastructure.crawler.providers;

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
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.repository.CurrencyRepository;
import com.uppdragsradarn.domain.repository.SkillRepository;
import com.uppdragsradarn.domain.repository.SourceTypeRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Provider for Developers Bay job listings. Developers Bay is a freelance community that posts
 * public assignments.
 */
@Component
@Slf4j
public class DevelopersBayProvider extends AbstractHttpProvider {

  private static final String LISTINGS_URL =
      "https://developersbay.se/tillgangliga-offentliga-uppdrag/";
  private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})/(\\d{2})/(\\d{2})");

  private final LocationService locationService;
  private final SkillRepository skillRepository;
  private final CurrencyRepository currencyRepository;
  private final SourceTypeRepository sourceTypeRepository;
  private final StatusTypeRepository statusTypeRepository;

  public DevelopersBayProvider(
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
    return "Developers Bay Provider";
  }

  @Override
  public boolean supports(Source source) {
    return source.getName().equalsIgnoreCase("Developers Bay")
        || (source.getBaseUrl() != null && source.getBaseUrl().contains("developersbay.se"));
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    logger.info("Fetching assignments from Developers Bay: {}", LISTINGS_URL);

    try {
      Document doc = fetchAndParseDocument(LISTINGS_URL);
      List<Assignment> assignments = new ArrayList<>();

      // Find all article elements that contain job listings
      Elements articles = doc.select("article");

      // If no articles, try alternative selectors
      if (articles.isEmpty()) {
        // Try to find job listings with Elementor post structure
        articles = doc.select(".elementor-post, .post");
      }

      logger.debug("Found {} potential job articles", articles.size());

      for (Element article : articles) {
        try {
          Assignment assignment = extractAssignmentFromArticle(article, source);
          if (assignment != null) {
            // Fetch additional details from the detail page
            if (assignment.getApplicationUrl() != null) {
              enrichAssignmentFromDetailPage(assignment);
            }
            assignments.add(assignment);
          }
        } catch (Exception e) {
          logger.warn("Error extracting assignment from article: {}", e.getMessage());
        }
      }

      // Also check for links that might be job postings
      if (assignments.isEmpty()) {
        Elements jobLinks = doc.select("a[href*='/20'][href*='developersbay.se']");
        Pattern jobUrlPattern = Pattern.compile(".*/\\d{4}/\\d{2}/\\d{2}/[^/]+/?$");

        for (Element link : jobLinks) {
          String href = link.attr("abs:href");
          if (jobUrlPattern.matcher(href).matches()) {
            try {
              Assignment assignment = fetchAndExtractFromUrl(href, source);
              if (assignment != null) {
                assignments.add(assignment);
              }
            } catch (Exception e) {
              logger.warn("Error extracting assignment from URL {}: {}", href, e.getMessage());
            }
          }
        }
      }

      logger.info("Extracted {} assignments from Developers Bay", assignments.size());
      return assignments;

    } catch (Exception e) {
      throw new CrawlerException("Failed to fetch Developers Bay listings", e);
    }
  }

  private Assignment extractAssignmentFromArticle(Element article, Source source) {
    Assignment assignment = new Assignment();
    assignment.setSource(source);
    assignment.setActive(true);
    assignment.setStatus(getOrCreateStatusType("ACTIVE", "ASSIGNMENT"));

    // Extract title
    Element titleElement = article.selectFirst("h2, h3, .elementor-post-title, .entry-title");
    if (titleElement == null) {
      return null;
    }

    String title = titleElement.text().trim();
    if (title.isEmpty()) {
      return null;
    }
    assignment.setTitle(title);

    // Extract link
    Element linkElement = titleElement.selectFirst("a");
    if (linkElement == null) {
      linkElement = article.selectFirst("a[href*='developersbay.se']");
    }

    if (linkElement != null) {
      String url = linkElement.attr("abs:href");
      assignment.setApplicationUrl(url);
      assignment.setExternalId(extractExternalIdFromUrl(url));
    } else {
      assignment.setExternalId(generateIdFromTitle(title));
    }

    // Extract date from URL or article metadata
    LocalDate publishDate = extractPublishDate(article, assignment.getApplicationUrl());
    if (publishDate != null) {
      // For now, we'll just log this - could be used for other date fields based on business logic
      logger.debug("Assignment {} was published on {}", title, publishDate);
    }

    // Set company name to Developers Bay by default
    assignment.setCompanyName("Developers Bay");

    // Set default location to Sweden
    locationService.processAssignmentLocation(assignment, "Sweden", source.getName());

    // Extract any preview description
    Element descElement = article.selectFirst(".entry-summary, .elementor-post__excerpt");
    if (descElement != null) {
      assignment.setDescription(descElement.text().trim());
    }

    return assignment;
  }

  private Assignment fetchAndExtractFromUrl(String url, Source source) {
    try {
      Document doc = fetchAndParseDocument(url);

      Assignment assignment = new Assignment();
      assignment.setSource(source);
      assignment.setActive(true);
      assignment.setStatus(getOrCreateStatusType("ACTIVE", "ASSIGNMENT"));
      assignment.setApplicationUrl(url);
      assignment.setExternalId(extractExternalIdFromUrl(url));

      // Extract title
      Element titleElement = doc.selectFirst("h1, .entry-title, .elementor-heading-title");
      if (titleElement == null) {
        return null;
      }
      assignment.setTitle(titleElement.text().trim());

      // Extract content
      Element contentElement =
          doc.selectFirst(".entry-content, .elementor-widget-theme-post-content");
      if (contentElement != null) {
        assignment.setDescription(cleanDescription(contentElement));
      }

      // Extract date
      LocalDate publishDate = extractPublishDate(doc, url);
      if (publishDate != null) {
        // For now, we'll just log this - could be used for other date fields based on business
        // logic
        logger.debug("Assignment {} was published on {}", assignment.getTitle(), publishDate);
      }

      // Extract location if mentioned
      String location = extractLocation(doc);
      locationService.processAssignmentLocation(assignment, location, source.getName());

      // Set company
      assignment.setCompanyName("Developers Bay");

      return assignment;

    } catch (Exception e) {
      logger.warn("Failed to fetch assignment from URL {}: {}", url, e.getMessage());
      return null;
    }
  }

  private void enrichAssignmentFromDetailPage(Assignment assignment) {
    try {
      Document doc = fetchAndParseDocument(assignment.getApplicationUrl());

      // Update description with full content
      Element contentElement =
          doc.selectFirst(".entry-content, .elementor-widget-theme-post-content");
      if (contentElement != null) {
        String fullDescription = cleanDescription(contentElement);
        if (fullDescription.length() > assignment.getDescription().length()) {
          assignment.setDescription(fullDescription);
        }
      }

      // Try to extract location from content
      String location = extractLocation(doc);
      if (!"Sweden".equals(location)) {
        locationService.processAssignmentLocation(
            assignment, location, assignment.getSource().getName());
      }

      // Try to extract skills from content
      Set<String> skills = extractSkillsFromContent(doc);
      for (String skillName : skills) {
        try {
          assignment.addSkill(findOrCreateSkill(skillName));
        } catch (Exception e) {
          logger.debug("Could not add skill: {}", skillName);
        }
      }

    } catch (Exception e) {
      logger.debug("Could not enrich assignment from detail page: {}", e.getMessage());
    }
  }

  private String cleanDescription(Element contentElement) {
    // Remove script and style elements
    contentElement.select("script, style").remove();

    // Get text with preserved line breaks
    String text = contentElement.wholeText().replaceAll("\\s+", " ").trim();

    // Limit length
    if (text.length() > 5000) {
      text = text.substring(0, 5000) + "...";
    }

    return text;
  }

  private LocalDate extractPublishDate(Element element, String url) {
    // Try to extract from URL
    if (url != null) {
      Matcher matcher = DATE_PATTERN.matcher(url);
      if (matcher.find()) {
        try {
          int year = Integer.parseInt(matcher.group(1));
          int month = Integer.parseInt(matcher.group(2));
          int day = Integer.parseInt(matcher.group(3));
          return LocalDate.of(year, month, day);
        } catch (Exception e) {
          logger.debug("Could not parse date from URL: {}", url);
        }
      }
    }

    // Try to extract from metadata
    Element timeElement = element.selectFirst("time[datetime]");
    if (timeElement != null) {
      String datetime = timeElement.attr("datetime");
      try {
        return LocalDate.parse(datetime.substring(0, 10));
      } catch (Exception e) {
        logger.debug("Could not parse datetime: {}", datetime);
      }
    }

    // Try common date patterns in text
    Element dateElement = element.selectFirst(".date, .post-date, .entry-date");
    if (dateElement != null) {
      String dateText = dateElement.text();
      try {
        // Try various date formats
        return parseSwedishDate(dateText);
      } catch (Exception e) {
        logger.debug("Could not parse date text: {}", dateText);
      }
    }

    return null;
  }

  private LocalDate parseSwedishDate(String dateText) {
    // Common Swedish date formats
    String[] patterns = {"d MMMM yyyy", "yyyy-MM-dd", "d MMM yyyy"};

    for (String pattern : patterns) {
      try {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, new Locale("sv", "SE"));
        return LocalDate.parse(dateText.trim(), formatter);
      } catch (DateTimeParseException e) {
        // Try next pattern
      }
    }

    throw new DateTimeParseException("Could not parse date", dateText, 0);
  }

  private String extractLocation(Document doc) {
    // Look for location patterns in the content
    String content = doc.text().toLowerCase();

    // Common location indicators
    String[] locationIndicators = {
      "ort:", "plats:", "location:", "stad:", "kommun:", "län:", "region:"
    };

    for (String indicator : locationIndicators) {
      int index = content.indexOf(indicator);
      if (index != -1) {
        // Extract the next word/phrase after the indicator
        String after = content.substring(index + indicator.length()).trim();
        String[] words = after.split("\\s+");
        if (words.length > 0) {
          // Capitalize first letter
          String location = words[0].substring(0, 1).toUpperCase() + words[0].substring(1);

          // Check if it's a known Swedish city
          if (isSwedishCity(location)) {
            return location;
          }
        }
      }
    }

    // Check for remote work indicators
    if (content.contains("distans") || content.contains("remote") || content.contains("hemma")) {
      return "Remote";
    }

    // Default to Sweden
    return "Sweden";
  }

  private boolean isSwedishCity(String city) {
    // Common Swedish cities
    Set<String> cities =
        Set.of(
            "stockholm",
            "göteborg",
            "gothenburg",
            "malmö",
            "uppsala",
            "västerås",
            "örebro",
            "linköping",
            "helsingborg",
            "jönköping",
            "norrköping",
            "lund",
            "umeå",
            "gävle",
            "borås",
            "södertälje",
            "eskilstuna",
            "karlstad",
            "täby",
            "sundsvall",
            "växjö");

    return cities.contains(city.toLowerCase());
  }

  private Set<String> extractSkillsFromContent(Document doc) {
    Set<String> skills = new HashSet<>();
    String content = doc.text().toLowerCase();

    // Common tech skills to look for
    String[] techSkills = {
      "java",
      "python",
      "javascript",
      "typescript",
      "react",
      "angular",
      "vue",
      "spring",
      "nodejs",
      "aws",
      "azure",
      "kubernetes",
      "docker",
      "devops",
      "ci/cd",
      "agile",
      "scrum",
      "sql",
      "nosql",
      "mongodb",
      "postgresql",
      "git",
      "jenkins",
      "terraform",
      "ansible",
      "linux",
      "windows",
      "microservices",
      "rest",
      "api",
      "frontend",
      "backend",
      "fullstack"
    };

    for (String skill : techSkills) {
      if (content.contains(skill)) {
        skills.add(skill.toUpperCase());
      }
    }

    return skills;
  }

  private String extractExternalIdFromUrl(String url) {
    // Extract the slug from the URL as external ID
    Pattern pattern = Pattern.compile(".*/([^/]+)/?$");
    Matcher matcher = pattern.matcher(url);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return String.valueOf(url.hashCode());
  }

  private String generateIdFromTitle(String title) {
    return title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
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
