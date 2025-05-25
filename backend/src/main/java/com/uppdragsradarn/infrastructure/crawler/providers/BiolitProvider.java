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
 * Provider for Biolit consulting assignments.
 * Biolit specializes in IT consulting with focus on BI, data, and development.
 */
@Component
@Slf4j
public class BiolitProvider extends AbstractHttpProvider {

  private static final String LISTINGS_URL = "https://biolit.se/konsultuppdrag/";
  private static final Pattern JOB_NUMBER_PATTERN = Pattern.compile("Uppdragsnummer:\\s*(\\d+)");
  private static final Pattern DATE_PATTERN = Pattern.compile("Inkom:\\s*(\\d{4}-\\d{2}-\\d{2})");
  private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
  
  private final LocationService locationService;
  private final SkillRepository skillRepository;
  private final CurrencyRepository currencyRepository;
  private final SourceTypeRepository sourceTypeRepository;
  private final StatusTypeRepository statusTypeRepository;

  public BiolitProvider(
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
    return "Biolit Provider";
  }

  @Override
  public boolean supports(Source source) {
    return source.getName().equalsIgnoreCase("Biolit")
        || (source.getBaseUrl() != null && source.getBaseUrl().contains("biolit.se"));
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    logger.info("Fetching assignments from Biolit: {}", LISTINGS_URL);
    
    try {
      Document doc = fetchAndParseDocument(LISTINGS_URL);
      List<Assignment> assignments = new ArrayList<>();
      
      // Find collapsible sections which contain job listings
      Elements collapsibleSections = doc.select(".collapsible");
      
      // If no collapsible sections, try alternative structure
      if (collapsibleSections.isEmpty()) {
        // Look for content sections with specific patterns
        Elements contentElements = doc.select(".content > div, .content > section");
        for (Element element : contentElements) {
          if (containsJobInfo(element)) {
            collapsibleSections.add(element);
          }
        }
      }
      
      logger.debug("Found {} potential job sections", collapsibleSections.size());
      
      for (Element section : collapsibleSections) {
        try {
          Assignment assignment = extractAssignment(section, source);
          if (assignment != null) {
            assignments.add(assignment);
          }
        } catch (Exception e) {
          logger.warn("Error extracting assignment from section: {}", e.getMessage());
        }
      }
      
      // Also check for assignments in text blocks with specific patterns
      if (assignments.isEmpty()) {
        assignments = extractFromTextBlocks(doc, source);
      }
      
      logger.info("Extracted {} assignments from Biolit", assignments.size());
      return assignments;
      
    } catch (Exception e) {
      throw new CrawlerException("Failed to fetch Biolit listings", e);
    }
  }

  private boolean containsJobInfo(Element element) {
    String text = element.text();
    return text.contains("Uppdragsnummer:") || text.contains("Inkom:");
  }

  private Assignment extractAssignment(Element section, Source source) {
    Assignment assignment = new Assignment();
    assignment.setSource(source);
    assignment.setActive(true);
    assignment.setStatus(getOrCreateStatusType("ACTIVE", "ASSIGNMENT"));
    
    // Extract title - usually in bold or strong tags
    Element titleElement = section.selectFirst("strong, b");
    if (titleElement == null) {
      // Try to extract from first line or heading
      String text = section.text();
      String[] lines = text.split("\n");
      if (lines.length > 0 && !lines[0].contains("Inkom:")) {
        assignment.setTitle(lines[0].trim());
      } else {
        return null;
      }
    } else {
      assignment.setTitle(titleElement.text().trim());
    }
    
    // Extract job number
    String jobNumber = extractJobNumber(section);
    if (jobNumber != null) {
      assignment.setExternalId(jobNumber);
    } else {
      assignment.setExternalId(generateIdFromTitle(assignment.getTitle()));
    }
    
    // Extract date
    LocalDate incomingDate = extractDate(section);
    if (incomingDate != null) {
      assignment.setPublishedDate(incomingDate);
    }
    
    // Extract contact email for application URL
    String email = extractEmail(section);
    if (email != null) {
      assignment.setApplicationUrl("mailto:" + email + "?subject=" + assignment.getTitle());
    }
    
    // Extract description - everything except metadata
    String description = extractDescription(section);
    assignment.setDescription(description);
    
    // Set company name
    assignment.setCompanyName("Biolit");
    
    // Extract location from description
    String location = extractLocationFromText(description);
    locationService.processAssignmentLocation(assignment, location, source.getName());
    
    // Extract skills from title and description
    Set<String> skills = extractSkills(assignment.getTitle(), description);
    for (String skillName : skills) {
      try {
        assignment.addSkill(findOrCreateSkill(skillName));
      } catch (Exception e) {
        logger.debug("Could not add skill: {}", skillName);
      }
    }
    
    return assignment;
  }

  private List<Assignment> extractFromTextBlocks(Document doc, Source source) {
    List<Assignment> assignments = new ArrayList<>();
    String content = doc.select(".content").text();
    
    // Split by job number pattern
    String[] blocks = content.split("(?=Uppdragsnummer:)");
    
    for (String block : blocks) {
      if (block.contains("Uppdragsnummer:")) {
        try {
          Assignment assignment = extractFromTextBlock(block, source);
          if (assignment != null) {
            assignments.add(assignment);
          }
        } catch (Exception e) {
          logger.warn("Error extracting assignment from text block: {}", e.getMessage());
        }
      }
    }
    
    return assignments;
  }

  private Assignment extractFromTextBlock(String block, Source source) {
    Assignment assignment = new Assignment();
    assignment.setSource(source);
    assignment.setActive(true);
    assignment.setStatus(getOrCreateStatusType("ACTIVE", "ASSIGNMENT"));
    
    // Extract title (usually first line before metadata)
    String[] lines = block.split("\n");
    String title = "";
    for (String line : lines) {
      line = line.trim();
      if (!line.isEmpty() && !line.contains("Inkom:") && !line.contains("Uppdragsnummer:")) {
        title = line;
        break;
      }
    }
    
    if (title.isEmpty()) {
      return null;
    }
    assignment.setTitle(title);
    
    // Extract metadata
    Matcher jobNumberMatcher = JOB_NUMBER_PATTERN.matcher(block);
    if (jobNumberMatcher.find()) {
      assignment.setExternalId(jobNumberMatcher.group(1));
    } else {
      assignment.setExternalId(generateIdFromTitle(title));
    }
    
    Matcher dateMatcher = DATE_PATTERN.matcher(block);
    if (dateMatcher.find()) {
      try {
        assignment.setPublishedDate(LocalDate.parse(dateMatcher.group(1)));
      } catch (Exception e) {
        logger.debug("Could not parse date: {}", dateMatcher.group(1));
      }
    }
    
    Matcher emailMatcher = EMAIL_PATTERN.matcher(block);
    if (emailMatcher.find()) {
      String email = emailMatcher.group(1);
      assignment.setApplicationUrl("mailto:" + email + "?subject=" + title);
    }
    
    // Clean description
    String description = block;
    description = description.replaceAll("Inkom:\\s*\\d{4}-\\d{2}-\\d{2}", "");
    description = description.replaceAll("Uppdragsnummer:\\s*\\d+", "");
    description = description.replaceAll(EMAIL_PATTERN.pattern(), "");
    description = description.replace(title, "").trim();
    
    assignment.setDescription(description);
    assignment.setCompanyName("Biolit");
    
    // Extract location
    String location = extractLocationFromText(description);
    locationService.processAssignmentLocation(assignment, location, source.getName());
    
    // Extract skills
    Set<String> skills = extractSkills(title, description);
    for (String skillName : skills) {
      try {
        assignment.addSkill(findOrCreateSkill(skillName));
      } catch (Exception e) {
        logger.debug("Could not add skill: {}", skillName);
      }
    }
    
    return assignment;
  }

  private String extractJobNumber(Element element) {
    String text = element.text();
    Matcher matcher = JOB_NUMBER_PATTERN.matcher(text);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  private LocalDate extractDate(Element element) {
    String text = element.text();
    Matcher matcher = DATE_PATTERN.matcher(text);
    if (matcher.find()) {
      try {
        return LocalDate.parse(matcher.group(1));
      } catch (Exception e) {
        logger.debug("Could not parse date: {}", matcher.group(1));
      }
    }
    return null;
  }

  private String extractEmail(Element element) {
    String text = element.text();
    Matcher matcher = EMAIL_PATTERN.matcher(text);
    if (matcher.find()) {
      return matcher.group(1);
    }
    
    // Also check for email links
    Element emailLink = element.selectFirst("a[href^=mailto:]");
    if (emailLink != null) {
      String mailto = emailLink.attr("href");
      return mailto.replace("mailto:", "").split("\\?")[0];
    }
    
    return null;
  }

  private String extractDescription(Element element) {
    String text = element.text();
    
    // Remove metadata
    text = text.replaceAll("Inkom:\\s*\\d{4}-\\d{2}-\\d{2}", "");
    text = text.replaceAll("Uppdragsnummer:\\s*\\d+", "");
    text = text.replaceAll("Kontakt:.*", "");
    text = text.replaceAll(EMAIL_PATTERN.pattern(), "");
    
    // Clean up
    text = text.replaceAll("\\s+", " ").trim();
    
    // Limit length
    if (text.length() > 5000) {
      text = text.substring(0, 5000) + "...";
    }
    
    return text;
  }

  private String extractLocationFromText(String text) {
    String lowercaseText = text.toLowerCase();
    
    // Common Swedish cities
    String[] cities = {
        "stockholm", "göteborg", "gothenburg", "malmö", "uppsala", "västerås",
        "örebro", "linköping", "helsingborg", "jönköping", "norrköping",
        "lund", "umeå", "gävle", "borås", "södertälje", "eskilstuna"
    };
    
    for (String city : cities) {
      if (lowercaseText.contains(city)) {
        return city.substring(0, 1).toUpperCase() + city.substring(1);
      }
    }
    
    // Check for remote indicators
    if (lowercaseText.contains("distans") || lowercaseText.contains("remote") || 
        lowercaseText.contains("hemma")) {
      return "Remote";
    }
    
    // Check for region mentions
    if (lowercaseText.contains("hela sverige")) {
      return "Sweden";
    }
    
    return "Sweden";
  }

  private Set<String> extractSkills(String title, String description) {
    Set<String> skills = new HashSet<>();
    String combined = (title + " " + description).toLowerCase();
    
    // BI and data related skills
    String[] dataSkills = {
        "bi", "business intelligence", "sas", "sql", "data warehouse", "dwh",
        "etl", "power bi", "tableau", "qlik", "qliksense", "qlikview",
        "oracle", "microsoft sql server", "mysql", "postgresql", "snowflake",
        "azure", "aws", "databricks", "spark", "hadoop", "python", "r",
        "data modeling", "dimensional modeling", "kimball", "inmon",
        "ssas", "ssis", "ssrs", "mdx", "dax", "data lake", "data governance"
    };
    
    // Development skills
    String[] devSkills = {
        "java", "c#", ".net", "javascript", "typescript", "react", "angular",
        "spring", "microservices", "api", "rest", "soap", "integration",
        "devops", "ci/cd", "docker", "kubernetes", "git", "agile", "scrum"
    };
    
    // Check for skills
    for (String skill : dataSkills) {
      if (combined.contains(skill)) {
        skills.add(skill.toUpperCase().replace(" ", "_"));
      }
    }
    
    for (String skill : devSkills) {
      if (combined.contains(skill)) {
        skills.add(skill.toUpperCase().replace(" ", "_"));
      }
    }
    
    return skills;
  }

  private String generateIdFromTitle(String title) {
    return "biolit-" + title.toLowerCase()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-|-$", "");
  }

  private com.uppdragsradarn.domain.model.StatusType getOrCreateStatusType(String name, String entityType) {
    return statusTypeRepository
        .findByNameAndEntityType(name, entityType)
        .orElseGet(() -> {
          var newStatus = com.uppdragsradarn.domain.model.StatusType.builder()
              .name(name)
              .entityType(entityType)
              .build();
          return statusTypeRepository.save(newStatus);
        });
  }

  private com.uppdragsradarn.domain.model.Skill findOrCreateSkill(String skillName) {
    return skillRepository
        .findByNameIgnoreCase(skillName)
        .orElseGet(() -> {
          var newSkill = com.uppdragsradarn.domain.model.Skill.builder()
              .name(skillName)
              .build();
          return skillRepository.save(newSkill);
        });
  }
}