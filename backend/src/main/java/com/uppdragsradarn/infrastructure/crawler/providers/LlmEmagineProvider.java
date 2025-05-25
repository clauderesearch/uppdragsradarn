package com.uppdragsradarn.infrastructure.crawler.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.CrawlerException;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.infrastructure.service.LlmJobExtractionService;

import lombok.extern.slf4j.Slf4j;

/**
 * LLM-based provider for Emagine jobs that uses the new extraction service. Demonstrates the hybrid
 * approach: simple list scraping + LLM detail extraction.
 */
@Component
@Slf4j
public class LlmEmagineProvider extends AbstractProvider {

  private static final String AJAX_ENDPOINT =
      "/wp-content/themes/emagine-theme/ajax/api-country.php";
  private static final String PUBLIC_URL_PATH = "/consultants/freelance-jobs/";
  private static final String DEFAULT_AJAX_DATA =
      "action=get_jobs_by_country&dats=%7B%22sorting%22%3A%22Id+DESC%22%2C%22skipCount%22%3A0%2C%22maxResultCount%22%3A50%2C%22filter%22%3A%22%22%2C%22primaryCategoryIds%22%3A%5B%5D%2C%22tenantIds%22%3A%5B2%5D%2C%22isFullyRemote%22%3Anull%2C%22countryId%22%3Anull%2C%22geoCoordinate%22%3A%7B%22latitude%22%3A0%2C%22longitude%22%3A0%7D%2C%22maxDistanceToWorkInKm%22%3A10000%2C%22permittedSortingFields%22%3A%5B%5D%2C%22language%22%3A%22sv%22%2C%22filters%22%3A%5B%5D%7D";

  private final int timeoutSeconds;
  private final String userAgent;
  private final int maxAssignments;
  private final String baseUrl;
  private final LlmJobExtractionService llmExtractionService;

  public LlmEmagineProvider(
      @Value("${app.crawler.timeout:30}") int timeoutSeconds,
      @Value(
              "${app.crawler.user-agent:Mozilla/5.0 (compatible; UppdragsRadarn/1.0; +https://uppdragsradarn.se/bot)}")
          String userAgent,
      @Value("${app.crawler.max-assignments:50}") int maxAssignments,
      @Value("${app.crawler.emagine.url:https://emagine-consulting.se}") String baseUrl,
      LlmJobExtractionService llmExtractionService) {
    this.timeoutSeconds = timeoutSeconds;
    this.userAgent = userAgent;
    this.maxAssignments = maxAssignments;
    this.baseUrl = baseUrl;
    this.llmExtractionService = llmExtractionService;
  }

  @Override
  public String getName() {
    return "LLM Emagine Provider";
  }

  @Override
  public boolean supports(Source source) {
    return source.getName().equalsIgnoreCase("Emagine Consulting (LLM)")
        || (source.getBaseUrl() != null && source.getBaseUrl().contains("emagine"));
  }

  @Override
  protected List<Assignment> fetchAndParse(Source source) throws CrawlerException {
    logger.info("Fetching assignments from Emagine using LLM extraction: {}", baseUrl);

    try {
      // Step 1: Get list of job URLs (keep the existing list fetching logic)
      List<String> jobUrls = fetchJobUrls();
      logger.info("Found {} job URLs from Emagine", jobUrls.size());

      // Step 2: Use LLM service to extract detailed information from each URL
      List<Assignment> assignments = new ArrayList<>();

      for (String jobUrl : jobUrls) {
        try {
          Assignment assignment =
              llmExtractionService.extractAssignmentFromUrl(jobUrl, source, "emagine");

          if (assignment != null) {
            assignments.add(assignment);
            logger.debug("Extracted assignment: {} from {}", assignment.getTitle(), jobUrl);
          }

          // Respect rate limits and avoid overwhelming the LLM service
          if (assignments.size() >= maxAssignments) {
            logger.info("Reached maximum assignment limit ({}), stopping", maxAssignments);
            break;
          }

          // Small delay to be respectful to the target site and LLM service
          Thread.sleep(1000);

        } catch (Exception e) {
          logger.warn("Failed to extract assignment from {}: {}", jobUrl, e.getMessage());
          // Continue with other URLs instead of failing completely
        }
      }

      logger.info(
          "Successfully extracted {} assignments from Emagine using LLM", assignments.size());
      return assignments;

    } catch (Exception e) {
      logger.error("Error fetching assignments from Emagine: {}", e.getMessage(), e);
      throw new CrawlerException("Error fetching assignments: " + e.getMessage(), e);
    }
  }

  /**
   * Fetches the list of job URLs using the existing AJAX endpoint approach. This part doesn't
   * change - we keep the simple list scraping.
   */
  private List<String> fetchJobUrls() throws Exception {
    try (CloseableHttpClient httpClient = createHttpClient()) {
      String ajaxUrl = baseUrl + AJAX_ENDPOINT;
      logger.debug("Making POST request to AJAX endpoint: {}", ajaxUrl);

      HttpPost httpPost = new HttpPost(ajaxUrl);
      httpPost.setHeader("User-Agent", userAgent);
      httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
      httpPost.setHeader("X-Requested-With", "XMLHttpRequest");
      httpPost.setHeader("Origin", baseUrl);
      httpPost.setHeader("Referer", baseUrl + PUBLIC_URL_PATH);

      httpPost.setEntity(
          new StringEntity(DEFAULT_AJAX_DATA, ContentType.APPLICATION_FORM_URLENCODED));

      try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
        int statusCode = response.getCode();
        if (statusCode != 200) {
          throw new Exception("Failed to fetch job list: Status code: " + statusCode);
        }

        HttpEntity entity = response.getEntity();
        if (entity == null) {
          throw new Exception("Empty response from Emagine");
        }

        String html = EntityUtils.toString(entity);
        return parseJobUrlsFromHtml(html);
      }
    }
  }

  private List<String> parseJobUrlsFromHtml(String html) {
    List<String> jobUrls = new ArrayList<>();

    try {
      Document doc = Jsoup.parse(html);
      Elements jobElements = doc.select(".row.consultants-list");

      for (Element jobElement : jobElements) {
        Element titleElement = jobElement.selectFirst(".consultants-list-title > a");
        if (titleElement != null) {
          String linkHref = titleElement.attr("href");
          if (linkHref != null && !linkHref.isEmpty()) {
            String fullUrl = baseUrl + linkHref;
            jobUrls.add(fullUrl);
          }
        }
      }

    } catch (Exception e) {
      logger.error("Error parsing job URLs from HTML: {}", e.getMessage(), e);
    }

    return jobUrls.stream().distinct().limit(maxAssignments).collect(Collectors.toList());
  }

  private CloseableHttpClient createHttpClient() {
    RequestConfig config =
        RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(timeoutSeconds))
            .setResponseTimeout(Timeout.ofSeconds(timeoutSeconds))
            .build();

    return HttpClients.custom().setDefaultRequestConfig(config).build();
  }
}
