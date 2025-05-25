package com.uppdragsradarn.infrastructure.crawler.config;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration class for managing crawler settings for each source. This configuration can be
 * stored in the source's configuration field or parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerConfiguration {

  // Basic crawler settings
  private String userAgent;
  private Integer timeoutSeconds;
  private Integer maxRetries;
  private Integer retryDelayMs;
  private String crawlInterval;

  // Scraping settings
  private Integer maxPages;
  private Integer pageSize;
  private Boolean detailRequired;
  private Boolean usePlaywright;

  // Rate limiting
  private Integer requestsPerMinute;
  private Integer burstLimit;

  // Authentication (if needed)
  private String authType; // none, apiKey, oauth, basic
  private String apiKey;
  private String username;
  private String password;

  // Selectors for standard web scraper
  private Map<String, String> selectors;

  // API settings
  private String apiEndpoint;
  private Map<String, String> apiHeaders;
  private Map<String, String> apiParams;

  // Parsing settings
  private String dateFormat;
  private String currencyFormat;
  private String rateFormat;

  // Feature flags
  private Boolean enableJavaScript;
  private Boolean followRedirects;
  private Boolean ignoreSslErrors;

  /** Creates a default configuration with sensible defaults. */
  public static CrawlerConfiguration defaultConfig() {
    return CrawlerConfiguration.builder()
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        .timeoutSeconds(30)
        .maxRetries(3)
        .retryDelayMs(1000)
        .crawlInterval("6h")
        .maxPages(5)
        .pageSize(50)
        .detailRequired(false)
        .usePlaywright(false)
        .requestsPerMinute(60)
        .burstLimit(10)
        .authType("none")
        .enableJavaScript(false)
        .followRedirects(true)
        .ignoreSslErrors(false)
        .build();
  }

  /** Merges this configuration with another, with the other configuration taking precedence. */
  public CrawlerConfiguration merge(CrawlerConfiguration other) {
    if (other == null) {
      return this;
    }

    return CrawlerConfiguration.builder()
        .userAgent(other.userAgent != null ? other.userAgent : this.userAgent)
        .timeoutSeconds(other.timeoutSeconds != null ? other.timeoutSeconds : this.timeoutSeconds)
        .maxRetries(other.maxRetries != null ? other.maxRetries : this.maxRetries)
        .retryDelayMs(other.retryDelayMs != null ? other.retryDelayMs : this.retryDelayMs)
        .crawlInterval(other.crawlInterval != null ? other.crawlInterval : this.crawlInterval)
        .maxPages(other.maxPages != null ? other.maxPages : this.maxPages)
        .pageSize(other.pageSize != null ? other.pageSize : this.pageSize)
        .detailRequired(other.detailRequired != null ? other.detailRequired : this.detailRequired)
        .usePlaywright(other.usePlaywright != null ? other.usePlaywright : this.usePlaywright)
        .requestsPerMinute(
            other.requestsPerMinute != null ? other.requestsPerMinute : this.requestsPerMinute)
        .burstLimit(other.burstLimit != null ? other.burstLimit : this.burstLimit)
        .authType(other.authType != null ? other.authType : this.authType)
        .apiKey(other.apiKey != null ? other.apiKey : this.apiKey)
        .username(other.username != null ? other.username : this.username)
        .password(other.password != null ? other.password : this.password)
        .selectors(other.selectors != null ? other.selectors : this.selectors)
        .apiEndpoint(other.apiEndpoint != null ? other.apiEndpoint : this.apiEndpoint)
        .apiHeaders(other.apiHeaders != null ? other.apiHeaders : this.apiHeaders)
        .apiParams(other.apiParams != null ? other.apiParams : this.apiParams)
        .dateFormat(other.dateFormat != null ? other.dateFormat : this.dateFormat)
        .currencyFormat(other.currencyFormat != null ? other.currencyFormat : this.currencyFormat)
        .rateFormat(other.rateFormat != null ? other.rateFormat : this.rateFormat)
        .enableJavaScript(
            other.enableJavaScript != null ? other.enableJavaScript : this.enableJavaScript)
        .followRedirects(
            other.followRedirects != null ? other.followRedirects : this.followRedirects)
        .ignoreSslErrors(
            other.ignoreSslErrors != null ? other.ignoreSslErrors : this.ignoreSslErrors)
        .build();
  }
}
