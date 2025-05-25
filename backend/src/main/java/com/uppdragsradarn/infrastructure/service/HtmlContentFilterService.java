package com.uppdragsradarn.infrastructure.service;

import java.io.InputStream;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import com.uppdragsradarn.infrastructure.crawler.config.ExtractionConfig;
import com.uppdragsradarn.infrastructure.crawler.config.ExtractionConfig.ContentFilterConfig;

/**
 * Service for filtering and optimizing HTML content before sending to LLM. Removes unnecessary
 * elements, limits token count, and applies readability algorithms.
 */
@Service
public class HtmlContentFilterService {

  private static final Logger logger = LoggerFactory.getLogger(HtmlContentFilterService.class);

  /**
   * Filters HTML content according to the provided configuration.
   *
   * @param htmlContent Raw HTML content from job detail page
   * @param config Configuration specifying how to filter the content
   * @return Filtered and optimized content ready for LLM processing
   */
  public String filterContent(String htmlContent, ExtractionConfig config) {
    if (htmlContent == null || htmlContent.isEmpty()) {
      return "";
    }

    ContentFilterConfig filterConfig = config.getContentFilter();
    if (filterConfig == null) {
      logger.warn("No content filter config provided, returning raw content");
      return truncateToTokenLimit(htmlContent, 2000);
    }

    try {
      Document doc = Jsoup.parse(htmlContent);

      // Step 1: Remove unwanted tags completely
      removeUnwantedTags(doc, filterConfig.getRemoveTags());

      // Step 2: Extract main content using selector
      Element contentElement = extractMainContent(doc, filterConfig.getContentSelector());
      if (contentElement == null) {
        logger.warn("No content found with selector: {}", filterConfig.getContentSelector());
        contentElement = doc.body(); // Fallback to body
      }

      // Step 3: Remove excluded elements
      removeExcludedElements(contentElement, filterConfig.getExcludeSelectors());

      // Step 4: Apply readability algorithm if enabled
      if (filterConfig.isUseReadability()) {
        contentElement = applyReadabilityAlgorithm(contentElement);
      }

      // Step 5: Convert to clean text/markdown
      String cleanContent = convertToCleanText(contentElement);

      // Step 6: Truncate to token limit
      cleanContent = truncateToTokenLimit(cleanContent, filterConfig.getMaxTokens());

      logger.debug(
          "Filtered content from {} to {} characters", htmlContent.length(), cleanContent.length());

      return cleanContent;

    } catch (Exception e) {
      logger.error("Error filtering HTML content: {}", e.getMessage(), e);
      // Fallback: return truncated raw content
      return truncateToTokenLimit(stripBasicHtml(htmlContent), filterConfig.getMaxTokens());
    }
  }

  /** Loads extraction configuration from YAML resource file. */
  public ExtractionConfig loadExtractionConfig(String providerId) {
    try {
      String configPath = "/extraction-configs/" + providerId + ".yaml";
      InputStream inputStream = getClass().getResourceAsStream(configPath);

      if (inputStream == null) {
        logger.warn("No extraction config found for provider: {}", providerId);
        return createDefaultConfig(providerId);
      }

      Yaml yaml = new Yaml();
      ExtractionConfig config = yaml.loadAs(inputStream, ExtractionConfig.class);

      if (config == null) {
        logger.warn("Failed to load config for provider: {}, using default", providerId);
        return createDefaultConfig(providerId);
      }

      return config;

    } catch (Exception e) {
      logger.error("Error loading extraction config for {}: {}", providerId, e.getMessage());
      return createDefaultConfig(providerId);
    }
  }

  /** Estimates token count for the given text (rough approximation). */
  public int estimateTokenCount(String text) {
    if (text == null) return 0;
    // Rough estimate: 1 token ≈ 4 characters for English/Swedish text
    return text.length() / 4;
  }

  private void removeUnwantedTags(Document doc, List<String> tagsToRemove) {
    if (tagsToRemove == null) return;

    for (String tag : tagsToRemove) {
      doc.select(tag).remove();
    }
  }

  private Element extractMainContent(Document doc, String contentSelector) {
    if (contentSelector == null || contentSelector.isEmpty()) {
      return doc.body();
    }

    Elements elements = doc.select(contentSelector);
    return elements.isEmpty() ? null : elements.first();
  }

  private void removeExcludedElements(Element contentElement, List<String> excludeSelectors) {
    if (excludeSelectors == null) return;

    for (String selector : excludeSelectors) {
      contentElement.select(selector).remove();
    }
  }

  private Element applyReadabilityAlgorithm(Element contentElement) {
    // Simple readability algorithm inspired by Mozilla's Readability
    // Prioritize elements with more text content and fewer links

    Elements candidates = contentElement.select("div, article, section, main, p");

    Element bestCandidate = null;
    int bestScore = 0;

    for (Element candidate : candidates) {
      int score = calculateReadabilityScore(candidate);
      if (score > bestScore) {
        bestScore = score;
        bestCandidate = candidate;
      }
    }

    return bestCandidate != null ? bestCandidate : contentElement;
  }

  private int calculateReadabilityScore(Element element) {
    String text = element.text();
    int textLength = text.length();

    // Base score on text length
    int score = textLength / 25; // Points per 25 characters

    // Bonus for paragraph tags
    score += element.select("p").size() * 25;

    // Penalty for links (usually navigation/ads)
    score -= element.select("a").size() * 25;

    // Penalty for small text elements (likely ads/metadata)
    if (textLength < 100) {
      score -= 50;
    }

    // Bonus for article-like content
    String className = element.className().toLowerCase();
    if (className.contains("content")
        || className.contains("article")
        || className.contains("job")) {
      score += 100;
    }

    return Math.max(0, score);
  }

  private String convertToCleanText(Element element) {
    // Convert HTML to a clean text format that preserves structure
    // but is easier for LLM to process

    StringBuilder result = new StringBuilder();

    // Extract title if present
    Elements titles = element.select("h1, h2, h3, .title, .job-title");
    for (Element title : titles) {
      result.append("# ").append(title.text().trim()).append("\n\n");
    }

    // Extract structured content
    Elements paragraphs = element.select("p, div, li");
    for (Element p : paragraphs) {
      String text = p.text().trim();
      if (!text.isEmpty() && text.length() > 10) { // Skip very short content
        result.append(text).append("\n\n");
      }
    }

    // Clean up extra whitespace
    return result.toString().replaceAll("\\n{3,}", "\n\n").trim();
  }

  private String stripBasicHtml(String html) {
    // Fallback: basic HTML stripping using Jsoup's built-in cleaner
    return Jsoup.clean(html, Safelist.basic()).replaceAll("\\s+", " ").trim();
  }

  private String truncateToTokenLimit(String text, int maxTokens) {
    int estimatedTokens = estimateTokenCount(text);

    if (estimatedTokens <= maxTokens) {
      return text;
    }

    // Truncate to approximately the right token count
    int maxChars = maxTokens * 4; // Rough conversion

    if (text.length() <= maxChars) {
      return text;
    }

    // Try to truncate at a word boundary
    String truncated = text.substring(0, maxChars);
    int lastSpace = truncated.lastIndexOf(' ');

    if (lastSpace > maxChars * 0.8) { // Only use word boundary if it's reasonably close
      truncated = truncated.substring(0, lastSpace);
    }

    return truncated + "...";
  }

  private ExtractionConfig createDefaultConfig(String providerId) {
    ExtractionConfig config = new ExtractionConfig();
    config.setProviderId(providerId);

    ContentFilterConfig filterConfig = new ContentFilterConfig();
    filterConfig.setContentSelector("main, .content, .job-content, body");
    filterConfig.setExcludeSelectors(
        List.of(
            "nav",
            "header",
            "footer",
            ".navigation",
            ".sidebar",
            ".ads",
            "script",
            "style",
            ".cookie",
            ".social"));
    filterConfig.setRemoveTags(List.of("script", "style", "noscript", "svg"));
    filterConfig.setMaxTokens(2000);
    filterConfig.setUseReadability(true);

    config.setContentFilter(filterConfig);

    ExtractionConfig.LlmConfig llmConfig = new ExtractionConfig.LlmConfig();
    llmConfig.setModel("gpt-4o-mini");
    llmConfig.setTemperature(0.1);
    llmConfig.setMaxResponseTokens(800);

    config.setLlmConfig(llmConfig);

    return config;
  }
}
