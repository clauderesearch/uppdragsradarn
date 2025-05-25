package com.uppdragsradarn.infrastructure.crawler.config;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Configuration for LLM-based content extraction from job detail pages. Defines how to filter and
 * process HTML content before sending to LLM.
 */
@Data
public class ExtractionConfig {

  /** Provider identifier (e.g., "emagine", "asociety") */
  private String providerId;

  /** Base URL for the provider */
  private String baseUrl;

  /** Configuration for HTML content filtering */
  private ContentFilterConfig contentFilter;

  /** LLM extraction configuration */
  private LlmConfig llmConfig;

  @Data
  public static class ContentFilterConfig {
    /** CSS selector to find the main content area */
    private String contentSelector;

    /** CSS selectors to exclude from content (ads, navigation, etc.) */
    private List<String> excludeSelectors;

    /** HTML tags to completely remove */
    private List<String> removeTags;

    /** Maximum number of tokens to send to LLM */
    private int maxTokens = 2000;

    /** Whether to use readability algorithm for content extraction */
    private boolean useReadability = false;
  }

  @Data
  public static class LlmConfig {
    /** Model to use for extraction (default: gpt-4o-mini) */
    private String model = "gpt-4o-mini";

    /** Temperature for LLM requests */
    private double temperature = 0.1;

    /** Maximum tokens in LLM response */
    private int maxResponseTokens = 1000;

    /** Custom prompt instructions for this provider */
    private String customInstructions;
  }

  /** Additional metadata extraction rules */
  private Map<String, String> metadataSelectors;
}
