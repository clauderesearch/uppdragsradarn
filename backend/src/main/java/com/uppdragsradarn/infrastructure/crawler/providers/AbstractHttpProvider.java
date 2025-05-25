package com.uppdragsradarn.infrastructure.crawler.providers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;

import com.uppdragsradarn.domain.model.CrawlerException;

import jakarta.annotation.PostConstruct;

/**
 * Abstract base class for HTTP-based content providers. Provides common functionality like HTTP
 * client management, request building, and retries.
 */
public abstract class AbstractHttpProvider extends AbstractProvider {

  protected static final int DEFAULT_TIMEOUT_SECONDS = 30;
  protected static final int DEFAULT_MAX_RETRIES = 3;
  protected static final int DEFAULT_RETRY_DELAY_MS = 1000;
  protected static final String DEFAULT_USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

  @Value("${app.crawler.timeout:30}")
  protected int timeoutSeconds;

  @Value("${app.crawler.max-retries:3}")
  protected int maxRetries;

  @Value("${app.crawler.retry-delay:1000}")
  protected int retryDelayMs;

  @Value("${app.crawler.user-agent:" + DEFAULT_USER_AGENT + "}")
  protected String userAgent;

  private HttpClient httpClient;

  protected AbstractHttpProvider() {
    // HTTP client is initialized lazily after Spring injection
  }

  @PostConstruct
  public void init() {
    // Initialize fields with default values if not set
    if (timeoutSeconds <= 0) {
      timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    }
    if (maxRetries <= 0) {
      maxRetries = DEFAULT_MAX_RETRIES;
    }
    if (retryDelayMs <= 0) {
      retryDelayMs = DEFAULT_RETRY_DELAY_MS;
    }
    if (userAgent == null || userAgent.isEmpty()) {
      userAgent = DEFAULT_USER_AGENT;
    }
  }

  /**
   * Gets the HTTP client instance, creating it if necessary. This method ensures the client is
   * created after Spring injection.
   *
   * @return HttpClient instance
   */
  protected HttpClient getHttpClient() {
    if (httpClient == null) {
      httpClient = createHttpClient();
    }
    return httpClient;
  }

  /**
   * Creates a configured HTTP client instance.
   *
   * @return configured HttpClient
   */
  protected HttpClient createHttpClient() {
    // Ensure timeout is at least 1 second to avoid IllegalArgumentException
    int effectiveTimeout = Math.max(timeoutSeconds, 1);
    if (timeoutSeconds <= 0) {
      effectiveTimeout = DEFAULT_TIMEOUT_SECONDS;
    }

    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(effectiveTimeout))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  /**
   * Fetches content from a URL with retry logic.
   *
   * @param url URL to fetch
   * @return HTML content as string
   * @throws CrawlerException if fetching fails after retries
   */
  protected String fetchWithRetry(String url) throws CrawlerException {
    return fetchWithRetry(url, Map.of());
  }

  /**
   * Fetches content from a URL with retry logic and custom headers.
   *
   * @param url URL to fetch
   * @param headers custom headers to include in the request
   * @return HTML content as string
   * @throws CrawlerException if fetching fails after retries
   */
  protected String fetchWithRetry(String url, Map<String, String> headers) throws CrawlerException {
    Exception lastException = null;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        logger.debug("Fetching URL (attempt {}/{}): {}", attempt, maxRetries, url);

        HttpRequest.Builder requestBuilder =
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("User-Agent", userAgent)
                .GET();

        // Add custom headers
        headers.forEach(requestBuilder::header);

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response =
            getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          return response.body();
        } else if (response.statusCode() == 429) {
          // Rate limited - wait longer
          long delayMs = calculateRateLimitDelay(attempt);
          logger.warn(
              "Rate limited (429) on attempt {}. Waiting {} ms before retry", attempt, delayMs);
          Thread.sleep(delayMs);
        } else {
          logger.warn("HTTP error {} for URL: {}", response.statusCode(), url);
          lastException = new CrawlerException("HTTP error: " + response.statusCode());
        }

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new CrawlerException("Interrupted while fetching URL: " + url, e);
      } catch (Exception e) {
        lastException = e;
        logger.warn("Error fetching URL on attempt {}: {}", attempt, e.getMessage());
      }

      // Wait before retry (except on last attempt)
      if (attempt < maxRetries) {
        try {
          long delayMs = calculateRetryDelay(attempt);
          logger.debug("Waiting {} ms before retry", delayMs);
          Thread.sleep(delayMs);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new CrawlerException("Interrupted during retry delay", e);
        }
      }
    }

    throw new CrawlerException(
        "Failed to fetch URL after " + maxRetries + " attempts: " + url, lastException);
  }

  /**
   * Parses HTML content into a Jsoup Document.
   *
   * @param html HTML content to parse
   * @return parsed Document
   */
  protected Document parseHtml(String html) {
    return Jsoup.parse(html);
  }

  /**
   * Fetches and parses a URL into a Jsoup Document.
   *
   * @param url URL to fetch and parse
   * @return parsed Document
   * @throws CrawlerException if fetching or parsing fails
   */
  protected Document fetchAndParseDocument(String url) throws CrawlerException {
    String html = fetchWithRetry(url);
    return parseHtml(html);
  }

  /**
   * Calculates retry delay with exponential backoff and jitter.
   *
   * @param attempt current attempt number
   * @return delay in milliseconds
   */
  protected long calculateRetryDelay(int attempt) {
    // Exponential backoff with jitter
    long baseDelay = retryDelayMs * (long) Math.pow(2, attempt - 1);
    long jitter = ThreadLocalRandom.current().nextLong(0, baseDelay / 4);
    return Math.min(baseDelay + jitter, 30000); // Max 30 seconds
  }

  /**
   * Calculates delay for rate limit scenarios.
   *
   * @param attempt current attempt number
   * @return delay in milliseconds
   */
  protected long calculateRateLimitDelay(int attempt) {
    // Longer delays for rate limiting
    return calculateRetryDelay(attempt) * 3;
  }

  /**
   * Extracts text from a CSS selector, returning null if not found.
   *
   * @param doc Jsoup document
   * @param selector CSS selector
   * @return extracted text or null
   */
  protected String selectText(Document doc, String selector) {
    var element = doc.selectFirst(selector);
    return element != null ? element.text().trim() : null;
  }

  /**
   * Extracts attribute value from a CSS selector, returning null if not found.
   *
   * @param doc Jsoup document
   * @param selector CSS selector
   * @param attribute attribute name
   * @return attribute value or null
   */
  protected String selectAttr(Document doc, String selector, String attribute) {
    var element = doc.selectFirst(selector);
    return element != null ? element.attr(attribute).trim() : null;
  }
}
