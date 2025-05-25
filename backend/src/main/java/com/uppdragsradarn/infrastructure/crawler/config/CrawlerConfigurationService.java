package com.uppdragsradarn.infrastructure.crawler.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uppdragsradarn.domain.model.Source;

/**
 * Service for managing crawler configurations for different sources. Loads configurations from
 * source settings and provides access to them.
 */
@Service
public class CrawlerConfigurationService {

  private static final Logger logger = LoggerFactory.getLogger(CrawlerConfigurationService.class);
  private final ObjectMapper objectMapper;

  public CrawlerConfigurationService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Gets the crawler configuration for a specific source. Combines default configuration with
   * source-specific settings.
   *
   * @param source the source to get configuration for
   * @return the merged configuration
   */
  public CrawlerConfiguration getConfiguration(Source source) {
    // Start with default configuration
    CrawlerConfiguration config = CrawlerConfiguration.defaultConfig();

    // Load configuration from source configuration field (JSON)
    if (source.getConfiguration() != null && !source.getConfiguration().isEmpty()) {
      try {
        Map<String, Object> configMap =
            objectMapper.readValue(
                source.getConfiguration(),
                objectMapper
                    .getTypeFactory()
                    .constructMapType(HashMap.class, String.class, Object.class));

        CrawlerConfiguration sourceConfig = convertMapToConfiguration(configMap);
        config = config.merge(sourceConfig);
      } catch (Exception e) {
        logger.warn(
            "Failed to parse configuration JSON for source {}: {}",
            source.getName(),
            e.getMessage());
      }
    }

    // Load additional settings from parameters
    if (source.getParameters() != null && !source.getParameters().isEmpty()) {
      CrawlerConfiguration paramConfig = convertParametersToConfiguration(source.getParameters());
      config = config.merge(paramConfig);
    }

    // Apply source type specific defaults
    config = applySourceTypeDefaults(config, source);

    return config;
  }

  /** Converts a map of configuration values to a CrawlerConfiguration object. */
  private CrawlerConfiguration convertMapToConfiguration(Map<String, Object> configMap) {
    CrawlerConfiguration.CrawlerConfigurationBuilder builder = CrawlerConfiguration.builder();

    // Basic settings
    if (configMap.containsKey("userAgent")) {
      builder.userAgent((String) configMap.get("userAgent"));
    }
    if (configMap.containsKey("timeout")) {
      builder.timeoutSeconds(parseInteger(configMap.get("timeout")));
    }
    if (configMap.containsKey("maxRetries")) {
      builder.maxRetries(parseInteger(configMap.get("maxRetries")));
    }
    if (configMap.containsKey("retryDelay")) {
      builder.retryDelayMs(parseInteger(configMap.get("retryDelay")));
    }
    if (configMap.containsKey("crawlInterval")) {
      builder.crawlInterval((String) configMap.get("crawlInterval"));
    }

    // API settings
    if (configMap.containsKey("apiUrl")) {
      builder.apiEndpoint((String) configMap.get("apiUrl"));
    }
    if (configMap.containsKey("apiKey")) {
      builder.apiKey((String) configMap.get("apiKey"));
    }

    // Feature flags
    if (configMap.containsKey("usePlaywright")) {
      builder.usePlaywright(parseBoolean(configMap.get("usePlaywright")));
    }
    if (configMap.containsKey("detailRequired")) {
      builder.detailRequired(parseBoolean(configMap.get("detailRequired")));
    }

    return builder.build();
  }

  /** Converts source parameters to configuration settings. */
  @SuppressWarnings("unchecked")
  private CrawlerConfiguration convertParametersToConfiguration(Map<String, Object> parameters) {
    CrawlerConfiguration.CrawlerConfigurationBuilder builder = CrawlerConfiguration.builder();

    // Extract configuration values from parameters
    Map<String, String> selectors = new HashMap<>();

    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      String key = entry.getKey();
      Object valueObj = entry.getValue();
      String value = valueObj != null ? valueObj.toString() : null;

      switch (key) {
        case "maxPages":
          builder.maxPages(parseInteger(value));
          break;
        case "pageSize":
          builder.pageSize(parseInteger(value));
          break;
        case "detailRequired":
          builder.detailRequired(parseBoolean(value));
          break;
        case "userAgent":
          builder.userAgent(value);
          break;
        case "timeout":
          builder.timeoutSeconds(parseInteger(value));
          break;
        case "crawlInterval":
          builder.crawlInterval(value);
          break;
        case "apiEndpoint":
          builder.apiEndpoint(value);
          break;
        default:
          // Collect selectors and other parameters
          if (key.endsWith("Selector") || key.contains(".")) {
            selectors.put(key, value);
          }
      }
    }

    if (!selectors.isEmpty()) {
      builder.selectors(selectors);
    }

    return builder.build();
  }

  /** Applies source type specific defaults to the configuration. */
  private CrawlerConfiguration applySourceTypeDefaults(CrawlerConfiguration config, Source source) {
    CrawlerConfiguration.CrawlerConfigurationBuilder builder =
        CrawlerConfiguration.builder()
            .userAgent(config.getUserAgent())
            .timeoutSeconds(config.getTimeoutSeconds())
            .maxRetries(config.getMaxRetries())
            .retryDelayMs(config.getRetryDelayMs())
            .crawlInterval(config.getCrawlInterval())
            .maxPages(config.getMaxPages())
            .pageSize(config.getPageSize())
            .detailRequired(config.getDetailRequired())
            .usePlaywright(config.getUsePlaywright())
            .requestsPerMinute(config.getRequestsPerMinute())
            .burstLimit(config.getBurstLimit())
            .authType(config.getAuthType())
            .apiKey(config.getApiKey())
            .username(config.getUsername())
            .password(config.getPassword())
            .selectors(config.getSelectors())
            .apiEndpoint(config.getApiEndpoint())
            .apiHeaders(config.getApiHeaders())
            .apiParams(config.getApiParams())
            .dateFormat(config.getDateFormat())
            .currencyFormat(config.getCurrencyFormat())
            .rateFormat(config.getRateFormat())
            .enableJavaScript(config.getEnableJavaScript())
            .followRedirects(config.getFollowRedirects())
            .ignoreSslErrors(config.getIgnoreSslErrors());

    // Apply source type specific defaults based on SourceType entity name
    if (source.getSourceType() != null) {
      String sourceTypeName = source.getSourceType().getName();

      switch (sourceTypeName) {
        case "API":
          builder.usePlaywright(false);
          builder.detailRequired(false);
          break;
        case "WEB_SCRAPER":
          // Web scrapers might need JavaScript rendering
          if (config.getEnableJavaScript() == null) {
            builder.enableJavaScript(false);
          }
          break;
        case "ASOCIETYGROUP":
          builder.usePlaywright(true);
          builder.detailRequired(true);
          break;
        case "EWORK":
        case "EMAGINE":
        case "EXPERIS":
          builder.usePlaywright(false);
          builder.detailRequired(false);
          break;
      }
    }

    return builder.build();
  }

  /** Safely parses an integer from an object. */
  private Integer parseInteger(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException e) {
      logger.warn("Failed to parse integer from value: {}", value);
      return null;
    }
  }

  /** Safely parses a boolean from an object. */
  private Boolean parseBoolean(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    String strValue = value.toString().toLowerCase();
    return "true".equals(strValue) || "1".equals(strValue);
  }
}
