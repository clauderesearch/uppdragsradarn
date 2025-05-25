package com.uppdragsradarn.infrastructure.crawler.providers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.CrawlerException;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.infrastructure.service.LlmJobExtractionService;

import lombok.extern.slf4j.Slf4j;

/**
 * Simplified LLM-based provider for A Society Group. Uses Playwright only for getting job URLs,
 * then LLM for detail extraction.
 */
@Component
@Slf4j
public class LlmASocietyProvider extends AbstractProvider {

  private static final String BASE_URL = "https://www.asocietygroup.com";
  private static final String LIST_URL = BASE_URL + "/en/uppdrag";
  private static final String DETAIL_BASE_URL = BASE_URL + "/en/uppdrag/";
  private static final String USER_AGENT =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

  // Only keep the job link pattern for URL extraction
  private static final java.util.regex.Pattern JOB_LINK_PATTERN =
      java.util.regex.Pattern.compile("/en/uppdrag/([^\"/?]+)");

  @Value("${app.crawler.max-assignments:50}")
  private int maxAssignments;

  private final LlmJobExtractionService llmExtractionService;

  public LlmASocietyProvider(LlmJobExtractionService llmExtractionService) {
    this.llmExtractionService = llmExtractionService;
  }

  @Override
  public String getName() {
    return "LLM A Society Provider";
  }

  @Override
  public boolean supports(Source source) {
    return source.getName().equalsIgnoreCase("A Society Group (LLM)")
        || (source.getBaseUrl() != null && source.getBaseUrl().contains("asocietygroup.com"));
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    logger.info("Fetching assignments from A Society Group using LLM extraction");

    try {
      // Step 1: Get job URLs using Playwright (simplified)
      List<String> jobUrls = fetchJobUrls();
      logger.info("Found {} job URLs from A Society Group", jobUrls.size());

      // Step 2: Use LLM service to extract detailed information
      List<Assignment> assignments = new ArrayList<>();

      for (String jobUrl : jobUrls) {
        try {
          Assignment assignment =
              llmExtractionService.extractAssignmentFromUrl(jobUrl, source, "asociety");

          if (assignment != null) {
            assignments.add(assignment);
            logger.debug("Extracted assignment: {} from {}", assignment.getTitle(), jobUrl);
          }

          if (assignments.size() >= maxAssignments) {
            logger.info("Reached maximum assignment limit ({}), stopping", maxAssignments);
            break;
          }

          // Small delay to be respectful
          Thread.sleep(1000);

        } catch (Exception e) {
          logger.warn("Failed to extract assignment from {}: {}", jobUrl, e.getMessage());
        }
      }

      logger.info(
          "Successfully extracted {} assignments from A Society Group using LLM",
          assignments.size());
      return assignments;

    } catch (Exception e) {
      logger.error("Error fetching assignments from A Society Group: {}", e.getMessage(), e);
      throw new CrawlerException("Error fetching assignments: " + e.getMessage(), e);
    }
  }

  /**
   * Simplified job URL fetching using Playwright. Only extracts URLs, no complex data extraction.
   */
  private List<String> fetchJobUrls() throws Exception {
    List<String> jobUrls = new ArrayList<>();
    Playwright playwright = null;
    Browser browser = null;

    try {
      playwright = Playwright.create();
      browser =
          playwright
              .chromium()
              .launch(new BrowserType.LaunchOptions().setHeadless(true).setTimeout(60000));

      BrowserContext context =
          browser.newContext(
              new Browser.NewContextOptions().setUserAgent(USER_AGENT).setLocale("sv-SE"));

      Page page = context.newPage();

      // Navigate to job listings
      page.navigate(LIST_URL);
      page.waitForLoadState(
          LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(30000));

      // Extract job URLs using simple regex
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

    } finally {
      if (browser != null) browser.close();
      if (playwright != null) playwright.close();
    }

    return jobUrls.stream().distinct().limit(maxAssignments).toList();
  }
}
