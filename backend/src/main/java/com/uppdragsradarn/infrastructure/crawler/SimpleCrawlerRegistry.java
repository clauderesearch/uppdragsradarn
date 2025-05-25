package com.uppdragsradarn.infrastructure.crawler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.CrawlerException;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.service.AssignmentCrawler;
import com.uppdragsradarn.domain.service.ContentProvider;

/** Simple registry that finds appropriate content providers for sources */
@Component
public class SimpleCrawlerRegistry {

  private static final Logger logger = LoggerFactory.getLogger(SimpleCrawlerRegistry.class);

  private final List<ContentProvider> providers;

  public SimpleCrawlerRegistry(List<ContentProvider> providers) {
    this.providers = providers;
    logger.info("Initialized SimpleCrawlerRegistry with {} providers", providers.size());
  }

  /**
   * Find a crawler for the given source
   *
   * @param source The source to find a crawler for
   * @return The crawler for the source, or null if no crawler is found
   */
  public AssignmentCrawler findCrawler(Source source) {
    logger.debug("Finding crawler for source: {}", source.getName());

    // Find provider that supports this source
    return providers.stream()
        .filter(provider -> provider.supports(source))
        .findFirst()
        .map(
            provider -> {
              logger.info("Using {} for source {}", provider.getName(), source.getName());
              return new ProviderAdapter(provider);
            })
        .orElse(null);
  }

  /** Adapter that converts ContentProvider to AssignmentCrawler */
  private static class ProviderAdapter implements AssignmentCrawler {

    private final ContentProvider provider;

    public ProviderAdapter(ContentProvider provider) {
      this.provider = provider;
    }

    @Override
    public boolean supports(Source source) {
      return provider.supports(source);
    }

    @Override
    public List<Assignment> fetchAssignments(Source source) {
      try {
        return provider.getAssignments(source);
      } catch (CrawlerException e) {
        return List.of();
      }
    }

    @Override
    public String getName() {
      return provider.getName();
    }
  }
}
