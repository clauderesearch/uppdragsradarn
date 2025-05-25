package com.uppdragsradarn.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.uppdragsradarn.application.config.StartupCrawlerInitializer;
import com.uppdragsradarn.application.service.CrawlerService;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.repository.AssignmentRepository;
import com.uppdragsradarn.domain.repository.SourceRepository;

@ExtendWith(MockitoExtension.class)
class StartupCrawlerInitializerTest {

  @Mock private CrawlerService crawlerService;

  @Mock private AssignmentRepository assignmentRepository;

  @Mock private SourceRepository sourceRepository;

  @Mock private Environment environment;

  private StartupCrawlerInitializer initializer;

  @BeforeEach
  void setUp() {
    initializer =
        new StartupCrawlerInitializer(
            crawlerService, assignmentRepository, sourceRepository, environment);
  }

  @Test
  void initializeCrawlersOnStartup_shouldTriggerCrawler_whenNoAssignmentsExist() {
    // Given
    when(environment.getProperty(eq("app.crawler.init-on-startup"), eq("true"))).thenReturn("true");
    when(assignmentRepository.count()).thenReturn(0L);

    // When
    initializer.initializeCrawlersOnStartup();

    // Then
    verify(crawlerService, times(1)).startScheduledCrawlerJobs();
    // Should not check for sources with no assignments since we're running all crawlers
    verify(sourceRepository, never()).findByActiveTrue();
  }

  @Test
  void initializeCrawlersOnStartup_shouldTriggerSpecificCrawlers_whenSourceHasNoAssignments() {
    // Given
    when(environment.getProperty(eq("app.crawler.init-on-startup"), eq("true"))).thenReturn("true");
    when(assignmentRepository.count()).thenReturn(10L);

    // Create a mock source
    Source source = mock(Source.class);
    UUID sourceId = UUID.randomUUID();
    when(source.getId()).thenReturn(sourceId);
    when(source.getName()).thenReturn("Test Source");

    List<Source> sources = Collections.singletonList(source);
    when(sourceRepository.findByActiveTrue()).thenReturn(sources);

    // This source has no assignments
    when(assignmentRepository.countBySource(source)).thenReturn(0L);

    // When
    initializer.initializeCrawlersOnStartup();

    // Then
    verify(crawlerService, never()).startScheduledCrawlerJobs(); // Should not run all crawlers
    verify(sourceRepository, times(1)).findByActiveTrue();
    verify(assignmentRepository, times(1)).countBySource(source);
    verify(crawlerService, times(1)).startCrawlerJob(sourceId); // Should run this specific crawler
  }

  @Test
  void initializeCrawlersOnStartup_shouldNotTriggerSpecificCrawlers_whenSourceHasAssignments() {
    // Given
    when(environment.getProperty(eq("app.crawler.init-on-startup"), eq("true"))).thenReturn("true");
    when(assignmentRepository.count()).thenReturn(10L);

    // Create a mock source
    Source source = mock(Source.class);
    // Using lenient for source.getId() since it's not actually called in this test
    // but it's part of our mock setup for consistency with other tests
    lenient().when(source.getId()).thenReturn(UUID.randomUUID());
    when(source.getName()).thenReturn("Test Source");

    List<Source> sources = Collections.singletonList(source);
    when(sourceRepository.findByActiveTrue()).thenReturn(sources);

    // This source has assignments
    when(assignmentRepository.countBySource(source)).thenReturn(5L);

    // When
    initializer.initializeCrawlersOnStartup();

    // Then
    verify(crawlerService, never()).startScheduledCrawlerJobs(); // Should not run all crawlers
    verify(sourceRepository, times(1)).findByActiveTrue();
    verify(assignmentRepository, times(1)).countBySource(source);
    verify(crawlerService, never())
        .startCrawlerJob(any(UUID.class)); // Should not run any specific crawler
  }

  @Test
  void initializeCrawlersOnStartup_shouldNotTriggerCrawler_whenDisabledInConfig() {
    // Given
    when(environment.getProperty(eq("app.crawler.init-on-startup"), eq("true")))
        .thenReturn("false");

    // When
    initializer.initializeCrawlersOnStartup();

    // Then
    verify(assignmentRepository, never()).count();
    verify(crawlerService, never()).startScheduledCrawlerJobs();
    verify(sourceRepository, never()).findByActiveTrue();
  }

  @Test
  void initializeCrawlersOnStartup_shouldHandleExceptions_whenCrawlerServiceFails() {
    // Given
    when(environment.getProperty(eq("app.crawler.init-on-startup"), eq("true"))).thenReturn("true");
    when(assignmentRepository.count()).thenReturn(0L);
    doThrow(new RuntimeException("Test exception"))
        .when(crawlerService)
        .startScheduledCrawlerJobs();

    // When
    initializer.initializeCrawlersOnStartup(); // Should not throw exception

    // Then
    verify(crawlerService, times(1)).startScheduledCrawlerJobs();
  }
}
