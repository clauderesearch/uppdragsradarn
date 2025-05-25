package com.uppdragsradarn.application.config;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

import com.uppdragsradarn.application.service.CrawlerService;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.repository.AssignmentRepository;
import com.uppdragsradarn.domain.repository.SourceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration to initialize crawler jobs on application startup if no assignments exist or if
 * there are sources with no assignments.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class StartupCrawlerInitializer {

  private final CrawlerService crawlerService;
  private final AssignmentRepository assignmentRepository;
  private final SourceRepository sourceRepository;
  private final Environment environment;

  /**
   * Event listener that triggers when the application is ready. Checks if there are any assignments
   * in the database, and if not, triggers the crawler jobs to populate initial data. Also checks
   * for sources that have no assignments and runs crawlers for them.
   */
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void initializeCrawlersOnStartup() {
    // Check if init-on-startup is enabled (default is true)
    boolean initOnStartup =
        Boolean.parseBoolean(environment.getProperty("app.crawler.init-on-startup", "true"));

    if (!initOnStartup) {
      log.info("Automatic crawler initialization on startup is disabled");
      return;
    }

    log.info("Checking if initial crawler run is needed...");
    long assignmentCount = assignmentRepository.count();

    if (assignmentCount == 0) {
      // No assignments at all - run all crawlers
      log.info("No assignments found in the database. Triggering all crawler jobs...");
      try {
        crawlerService.startScheduledCrawlerJobs();
        log.info("Initial crawler jobs started successfully");
      } catch (Exception e) {
        log.error("Failed to start initial crawler jobs", e);
      }
    } else {
      log.info(
          "Found {} existing assignments. Checking for sources with no assignments...",
          assignmentCount);

      // Get all active sources
      List<Source> activeSources = sourceRepository.findByActiveTrue();

      // Check each source for assignments
      for (Source source : activeSources) {
        long sourceAssignmentCount = assignmentRepository.countBySource(source);

        if (sourceAssignmentCount == 0) {
          log.info(
              "No assignments found for source: {}. Starting crawler job...", source.getName());
          try {
            UUID sourceId = source.getId();
            crawlerService.startCrawlerJob(sourceId);
            log.info("Started crawler job for source: {}", source.getName());
          } catch (Exception e) {
            log.error(
                "Failed to start crawler job for source {}: {}",
                source.getName(),
                e.getMessage(),
                e);
          }
        } else {
          log.info(
              "Found {} assignments for source: {}. Skipping crawler run.",
              sourceAssignmentCount,
              source.getName());
        }
      }
    }
  }
}
