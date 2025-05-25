package com.uppdragsradarn.infrastructure.crawler.providers;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.CrawlerException;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.service.ContentProvider;
import com.uppdragsradarn.parser.DescriptionParser;
import com.uppdragsradarn.parser.model.ParseResult;

/** Base class for content providers with common functionality */
public abstract class AbstractProvider implements ContentProvider {

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private final DescriptionParser descriptionParser = new DescriptionParser();

  @Override
  @Transactional
  public List<Assignment> getAssignments(Source source) throws CrawlerException {
    logger.info("Starting provider: {} for source: {}", getName(), source.getName());

    try {
      // Pre-load assignments to avoid lazy loading issues
      source.getAssignments().size();
      
      // Fetch and parse assignments
      List<Assignment> assignments = fetchAndParse(source);
      logger.info(
          "Successfully fetched {} assignments from {}", assignments.size(), source.getName());

      // Set source, timestamps, and convert HTML to clean text for all assignments
      assignments.forEach(
          assignment -> {
            // Set source
            assignment.setSource(source);

            // Set timestamps if not already set
            if (assignment.getCreatedAt() == null) {
              assignment.setCreatedAt(LocalDateTime.now());
            }
            if (assignment.getUpdatedAt() == null) {
              assignment.setUpdatedAt(LocalDateTime.now());
            }

            // Parse description into clean Markdown and detect PII
            if (assignment.getDescription() != null && !assignment.getDescription().isEmpty()) {
              ParseResult parseResult =
                  descriptionParser.parseWithPIIDetection(assignment.getDescription());

              // Set the parsed description
              assignment.setDescription(parseResult.getParsedContent());

              // Set PII detection flags
              assignment.setNeedsManualReview(parseResult.isNeedsManualReview());

              // If PII was detected, store the details
              if (parseResult.getPiiDetectionResult() != null
                  && parseResult.getPiiDetectionResult().containsPII()) {
                // Store a summary of detected PII types
                StringBuilder piiSummary = new StringBuilder();
                parseResult
                    .getPiiDetectionResult()
                    .getMatches()
                    .forEach(
                        match -> {
                          if (piiSummary.length() > 0) {
                            piiSummary.append(", ");
                          }
                          piiSummary.append(match.getType()).append(": ").append(match.getValue());
                        });
                assignment.setPiiDetected(piiSummary.toString());

                logger.warn("PII detected in assignment {}: {}", assignment.getTitle(), piiSummary);
              }
            }
          });

      return assignments;
    } catch (Exception e) {
      logger.error("Error in {} for source {}: {}", getName(), source.getName(), e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  /**
   * Implementation-specific fetching and parsing logic
   *
   * @param source The source to fetch assignments from
   * @return List of fetched and parsed assignments
   * @throws CrawlerException if there's an error during fetching or parsing
   */
  protected abstract List<Assignment> fetchAndParse(Source source) throws CrawlerException;
}
