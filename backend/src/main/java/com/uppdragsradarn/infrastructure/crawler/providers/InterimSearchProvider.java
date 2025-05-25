package com.uppdragsradarn.infrastructure.crawler.providers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
 * Provider for Interim Search job listings. Interim Search specializes in interim management and
 * executive positions.
 */
@Component
@Slf4j
public class InterimSearchProvider extends AbstractHttpProvider {

  private static final String LISTINGS_URL = "https://www.interimsearch.com/publika-uppdrag/";
  private static final Pattern JOB_ID_PATTERN = Pattern.compile("jobb-ID:\\s*(\\d+)");
  private static final Pattern START_DATE_PATTERN = Pattern.compile("Start:\\s*([^\\n]+)");
  private static final Pattern LOCATION_PATTERN = Pattern.compile("Ort:\\s*([^\\n]+)");

  private final LocationService locationService;
  private final SkillRepository skillRepository;
  private final CurrencyRepository currencyRepository;
  private final SourceTypeRepository sourceTypeRepository;
  private final StatusTypeRepository statusTypeRepository;

  public InterimSearchProvider(
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
    return "Interim Search Provider";
  }

  @Override
  public boolean supports(Source source) {
    return source.getName().equalsIgnoreCase("Interim Search")
        || (source.getBaseUrl() != null && source.getBaseUrl().contains("interimsearch.com"));
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    logger.info("Fetching assignments from Interim Search: {}", LISTINGS_URL);

    try {
      Document doc = fetchAndParseDocument(LISTINGS_URL);
      List<Assignment> assignments = new ArrayList<>();

      // Find job listings - they appear to be in article or div elements
      Elements jobElements = doc.select(".publika-uppdrag article, .uppdrag-item, article.post");

      // If no specific job elements found, try more generic selectors
      if (jobElements.isEmpty()) {
        // Look for h6 tags that might be job titles
        Elements titleElements = doc.select("h6");
        for (Element titleElement : titleElements) {
          Element parent = titleElement.parent();
          if (parent != null && containsJobInfo(parent)) {
            jobElements.add(parent);
          }
        }
      }

      logger.debug("Found {} job elements", jobElements.size());

      for (Element jobElement : jobElements) {
        try {
          Assignment assignment = extractAssignment(jobElement, source);
          if (assignment != null) {
            // Try to enrich from detail page if URL is available
            if (assignment.getApplicationUrl() != null) {
              enrichFromDetailPage(assignment);
            }
            assignments.add(assignment);
          }
        } catch (Exception e) {
          logger.warn("Error extracting assignment: {}", e.getMessage());
        }
      }

      // Handle dynamic loading - check for "Visa fler" button
      Element loadMoreButton = doc.selectFirst("a:contains(Visa fler), button:contains(Visa fler)");
      if (loadMoreButton != null) {
        logger.info("Note: This site may have more jobs available via dynamic loading");
      }

      logger.info("Extracted {} assignments from Interim Search", assignments.size());
      return assignments;

    } catch (Exception e) {
      throw new CrawlerException("Failed to fetch Interim Search listings", e);
    }
  }

  private boolean containsJobInfo(Element element) {
    String text = element.text().toLowerCase();
    return text.contains("ort:") || text.contains("start:") || text.contains("jobb-id:");
  }

  private Assignment extractAssignment(Element jobElement, Source source) {
    Assignment assignment = new Assignment();
    assignment.setSource(source);
    assignment.setActive(true);
    assignment.setStatus(getOrCreateStatusType("ACTIVE", "ASSIGNMENT"));

    // Extract title - look for h6 or similar heading
    Element titleElement = jobElement.selectFirst("h6, h5, h4, h3, .job-title");
    if (titleElement == null) {
      return null;
    }

    String title = titleElement.text().trim();
    if (title.isEmpty() || title.equalsIgnoreCase("Konfidentiellt")) {
      // For confidential assignments, try to generate a meaningful title
      String jobId = extractJobId(jobElement);
      if (jobId != null) {
        title = "Konfidentiellt uppdrag #" + jobId;
      } else {
        return null;
      }
    }
    assignment.setTitle(title);

    // Extract link
    Element linkElement = titleElement.selectFirst("a");
    if (linkElement == null) {
      linkElement = jobElement.selectFirst("a[href*='/uppdrag/']");
    }

    if (linkElement != null) {
      String url = linkElement.attr("abs:href");
      assignment.setApplicationUrl(url);
      assignment.setExternalId(extractIdFromUrl(url));
    } else {
      // Use job ID as external ID if available
      String jobId = extractJobId(jobElement);
      if (jobId != null) {
        assignment.setExternalId(jobId);
      } else {
        assignment.setExternalId(generateIdFromTitle(title));
      }
    }

    // Extract location
    String location = extractLocation(jobElement);
    locationService.processAssignmentLocation(assignment, location, source.getName());

    // Extract start date
    String startDate = extractStartDate(jobElement);
    if (startDate != null) {
      assignment.setStartDate(parseStartDate(startDate));
    }

    // Set company name
    assignment.setCompanyName("Interim Search");

    // Extract any description preview
    String description = extractDescription(jobElement);
    assignment.setDescription(description);

    return assignment;
  }

  private void enrichFromDetailPage(Assignment assignment) {
    try {
      Document doc = fetchAndParseDocument(assignment.getApplicationUrl());

      // Extract full description
      Element contentElement =
          doc.selectFirst(".entry-content, .job-description, article .content");
      if (contentElement != null) {
        String fullDescription = cleanDescription(contentElement);
        if (fullDescription.length() > assignment.getDescription().length()) {
          assignment.setDescription(fullDescription);
        }
      }

      // Try to extract more detailed information
      extractDetailedInfo(doc, assignment);

      // Extract skills from content
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

  private String extractJobId(Element element) {
    String text = element.text();
    Matcher matcher = JOB_ID_PATTERN.matcher(text);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  private String extractLocation(Element element) {
    String text = element.text();
    Matcher matcher = LOCATION_PATTERN.matcher(text);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }

    // Look for location in specific elements
    Element locationElement = element.selectFirst(".location, .ort, span:contains(Ort:)");
    if (locationElement != null) {
      String location = locationElement.text().replaceFirst("Ort:\\s*", "").trim();
      if (!location.isEmpty()) {
        return location;
      }
    }

    return "Sweden";
  }

  private String extractStartDate(Element element) {
    String text = element.text();
    Matcher matcher = START_DATE_PATTERN.matcher(text);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }

  private LocalDate parseStartDate(String startDateText) {
    if (startDateText == null) return null;

    String normalized = startDateText.toLowerCase();

    // Handle immediate start
    if (normalized.contains("omgående")
        || normalized.contains("asap")
        || normalized.contains("snarast")) {
      return LocalDate.now();
    }

    // Handle relative dates
    if (normalized.contains("vecka") || normalized.contains("week")) {
      Pattern weekPattern = Pattern.compile("(\\d+)\\s*(vecka|veckor|week|weeks)");
      Matcher matcher = weekPattern.matcher(normalized);
      if (matcher.find()) {
        int weeks = Integer.parseInt(matcher.group(1));
        return LocalDate.now().plusWeeks(weeks);
      }
    }

    // Try to parse specific date formats
    try {
      // Swedish date format
      DateTimeFormatter formatter =
          DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("sv", "SE"));
      return LocalDate.parse(startDateText, formatter);
    } catch (Exception e) {
      // Try other formats
    }

    try {
      return LocalDate.parse(startDateText);
    } catch (Exception e) {
      logger.debug("Could not parse start date: {}", startDateText);
    }

    return null;
  }

  private String extractDescription(Element element) {
    // Remove the metadata lines (Ort:, Start:, etc.)
    String text = element.text();

    // Remove known metadata patterns
    text = text.replaceAll("Ort:\\s*[^\\n]+", "");
    text = text.replaceAll("Start:\\s*[^\\n]+", "");
    text = text.replaceAll("jobb-ID:\\s*\\d+", "");
    text = text.replaceAll("Konfidentiellt", "");

    // Clean up and trim
    text = text.replaceAll("\\s+", " ").trim();

    // If too short, return a default description
    if (text.length() < 20) {
      return "Interim management position. See details on website.";
    }

    return text;
  }

  private void extractDetailedInfo(Document doc, Assignment assignment) {
    // Look for structured data
    Elements infoRows = doc.select("dl dt, dl dd, .job-info dt, .job-info dd");

    String currentLabel = null;
    for (Element element : infoRows) {
      if (element.tagName().equals("dt")) {
        currentLabel = element.text().toLowerCase();
      } else if (element.tagName().equals("dd") && currentLabel != null) {
        String value = element.text().trim();

        switch (currentLabel) {
          case "bransch:":
          case "industry:":
            // Could extract industry info if needed
            break;
          case "omfattning:":
          case "extent:":
            // Could extract extent info (full-time, part-time)
            break;
          case "längd:":
          case "duration:":
            // Could extract duration info
            break;
        }
      }
    }
  }

  private String cleanDescription(Element contentElement) {
    // Clone to avoid modifying original
    Element clone = contentElement.clone();

    // Remove unwanted elements
    clone.select("script, style, nav, .breadcrumb").remove();

    // Get text with line breaks preserved
    String text = clone.wholeText().replaceAll("\\n{3,}", "\n\n").trim();

    // Limit length
    if (text.length() > 5000) {
      text = text.substring(0, 5000) + "...";
    }

    return text;
  }

  private Set<String> extractSkillsFromContent(Document doc) {
    Set<String> skills = new HashSet<>();
    String content = doc.text().toLowerCase();

    // Management and leadership skills relevant to interim positions
    String[] managementSkills = {
      "leadership",
      "ledarskap",
      "management",
      "change management",
      "förändringsledning",
      "project management",
      "projektledning",
      "agile",
      "lean",
      "six sigma",
      "strategic planning",
      "strategisk planering",
      "operations",
      "finance",
      "hr",
      "quality management",
      "kvalitetsledning",
      "supply chain",
      "logistics",
      "sales",
      "marketing",
      "it management",
      "digital transformation"
    };

    for (String skill : managementSkills) {
      if (content.contains(skill)) {
        skills.add(skill.toUpperCase().replace(" ", "_"));
      }
    }

    return skills;
  }

  private String extractIdFromUrl(String url) {
    Pattern pattern = Pattern.compile("/uppdrag/([^/]+)/?");
    Matcher matcher = pattern.matcher(url);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return String.valueOf(url.hashCode());
  }

  private String generateIdFromTitle(String title) {
    return "interim-" + title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
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
