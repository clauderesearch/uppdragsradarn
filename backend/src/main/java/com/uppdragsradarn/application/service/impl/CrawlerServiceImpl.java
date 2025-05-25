package com.uppdragsradarn.application.service.impl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uppdragsradarn.application.service.AssignmentService;
import com.uppdragsradarn.application.service.CrawlerService;
import com.uppdragsradarn.application.service.LocationService;
import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.CrawlerException;
import com.uppdragsradarn.domain.model.CrawlerJobExecution;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.StatusType;
import com.uppdragsradarn.domain.repository.AssignmentRepository;
import com.uppdragsradarn.domain.repository.CrawlerJobRepository;
import com.uppdragsradarn.domain.repository.SourceRepository;
import com.uppdragsradarn.domain.repository.StatusTypeRepository;
import com.uppdragsradarn.domain.service.AssignmentCrawler;
import com.uppdragsradarn.infrastructure.crawler.SimpleCrawlerRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Implementation of the CrawlerService interface */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerServiceImpl implements CrawlerService {

  private final SourceRepository sourceRepository;
  private final AssignmentRepository assignmentRepository;
  private final AssignmentService assignmentService;
  private final LocationService locationService;
  private final ThreadPoolTaskScheduler taskScheduler;
  private final CrawlerJobRepository crawlerJobRepository;
  private final SimpleCrawlerRegistry crawlerRegistry;
  private final StatusTypeRepository statusTypeRepository;

  // In-memory tracking of currently running jobs
  private final Map<String, Future<?>> runningJobs = new ConcurrentHashMap<>();

  @Override
  @Transactional
  public CrawlerJobExecution startCrawlerJob(UUID sourceId) {
    Source source =
        sourceRepository
            .findById(sourceId)
            .orElseThrow(
                () -> new IllegalArgumentException("Source not found with ID: " + sourceId));

    // Create a unique job ID
    String jobId = UUID.randomUUID().toString();

    // Create the job execution record
    CrawlerJobExecution jobExecution =
        CrawlerJobExecution.builder()
            .id(jobId)
            .source(source)
            .sourceName(source.getName())
            .startTime(LocalDateTime.now())
            .status(getStatusType("RUNNING"))
            .build();

    // Persist the job execution to the database
    jobExecution = crawlerJobRepository.save(jobExecution);

    // Find a suitable crawler using the registry
    AssignmentCrawler crawler = crawlerRegistry.findCrawler(source);
    if (crawler == null) {
      updateJobStatus(
          jobId,
          getStatusType("FAILED"),
          "No suitable crawler found for source: " + source.getName());
      return jobExecution;
    }

    // Schedule the crawler job
    Future<?> future = taskScheduler.submit(() -> executeCrawlerJob(jobId, source, crawler));
    runningJobs.put(jobId, future);

    log.info("Started crawler job {} for source: {}", jobId, source.getName());
    return jobExecution;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CrawlerJobExecution> getCrawlerJobStatus(String jobId) {
    return crawlerJobRepository.findById(jobId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CrawlerJobExecution> getRecentCrawlerJobs(int limit) {
    return crawlerJobRepository.findRecent(
        org.springframework.data.domain.PageRequest.of(0, limit));
  }

  @Override
  @Transactional(readOnly = true)
  public List<CrawlerJobExecution> getSourceCrawlerJobs(UUID sourceId, int limit) {
    return crawlerJobRepository.findBySourceId(
        sourceId, org.springframework.data.domain.PageRequest.of(0, limit));
  }

  @Override
  @Scheduled(cron = "${app.crawler.schedule:0 0 */6 * * *}") // Default: every 6 hours
  public void startScheduledCrawlerJobs() {
    log.info("Starting scheduled crawler jobs");
    List<Source> activeSources = sourceRepository.findByActiveTrue();

    for (Source source : activeSources) {
      try {
        startCrawlerJob(source.getId());
      } catch (Exception e) {
        log.error(
            "Error scheduling crawler job for source {}: {}", source.getName(), e.getMessage(), e);
      }
    }
  }

  @Override
  public boolean cancelCrawlerJob(String jobId) {
    Future<?> job = runningJobs.get(jobId);
    if (job != null && !job.isDone() && !job.isCancelled()) {
      boolean cancelled = job.cancel(true);
      if (cancelled) {
        updateJobStatus(jobId, getStatusType("CANCELLED"), null);
        runningJobs.remove(jobId);
      }
      return cancelled;
    }
    return false;
  }

  /**
   * Execute a crawler job for a specific source
   *
   * @param jobId The job ID
   * @param source The source to crawl
   * @param crawler The crawler to use
   */
  private void executeCrawlerJob(String jobId, Source source, AssignmentCrawler crawler) {
    log.info(
        "Executing crawler job {} for source: {} using crawler: {}",
        jobId,
        source.getName(),
        crawler.getName());

    try {
      // Process and save the assignments with immediate persistence
      AtomicInteger created = new AtomicInteger(0);
      AtomicInteger updated = new AtomicInteger(0);
      List<String> processedIds = new ArrayList<>();

      // Track found assignments count (we'll process them one by one)
      AtomicInteger foundCount = new AtomicInteger(0);

      try {
        // Fetch and process assignments from the source
        List<Assignment> assignments = crawler.fetchAssignments(source);
        foundCount.set(assignments.size());

        for (Assignment assignment : assignments) {
          try {
            // Set the source before saving
            assignment.setSource(source);

            // Process this individual assignment
            processAssignment(assignment, source, created, updated);

            // Add to processed list
            processedIds.add(assignment.getExternalId());

            // Update job progress periodically (every 10 assignments)
            if ((created.get() + updated.get()) % 10 == 0) {
              updateJobProgress(
                  jobId, foundCount.get(), created.get(), updated.get(), processedIds);
            }
          } catch (Exception e) {
            log.error("Error processing individual assignment: {}", e.getMessage(), e);
            // Continue with next assignment despite error
          }
        }
      } catch (CrawlerException e) {
        // Log the error but don't fail the job if we have processed some assignments
        log.error(
            "Error fetching assignments from source {}: {}", source.getName(), e.getMessage(), e);
        if (created.get() == 0 && updated.get() == 0) {
          // Rethrow the exception if we haven't processed any assignments
          throw e;
        }
      }

      // Update the job status in the database
      updateJobProgress(jobId, foundCount.get(), created.get(), updated.get(), processedIds);

      updateJobStatus(jobId, getStatusType("SUCCESS"), null);
      log.info(
          "Crawler job {} completed successfully. Found: {}, Created: {}, Updated: {}",
          jobId,
          foundCount.get(),
          created,
          updated);

    } catch (Exception e) {
      log.error("Unexpected error in crawler job {}: {}", jobId, e.getMessage(), e);
      updateJobStatus(jobId, getStatusType("FAILED"), "Unexpected error: " + e.getMessage());
    } finally {
      runningJobs.remove(jobId);
    }
  }

  /**
   * Process and save a single assignment
   *
   * @param assignment The assignment to process
   * @param source The source
   * @param created Counter for created assignments
   * @param updated Counter for updated assignments
   */
  @Transactional
  protected void processAssignment(
      Assignment assignment, Source source, AtomicInteger created, AtomicInteger updated) {
    // Try to find an existing assignment with the same external ID
    Optional<Assignment> existingOpt =
        assignmentRepository.findBySourceAndExternalId(source, assignment.getExternalId());

    if (existingOpt.isPresent()) {
      // Update existing assignment
      Assignment existing = existingOpt.get();
      // Copy relevant fields from new assignment to existing one
      updateAssignmentFields(existing, assignment);
      assignmentService.updateAssignment(existing);
      updated.incrementAndGet();

      // Log update of description
      if (existing.getDescription() != null) {
        log.debug(
            "Updated assignment with description (length: {}): {}",
            existing.getDescription().length(),
            existing.getExternalId());
      } else {
        log.warn("Updated assignment but description is null: {}", existing.getExternalId());
      }
    } else {
      // Create new assignment
      Assignment created_assignment = assignmentService.createAssignment(assignment);
      created.incrementAndGet();

      // Location normalization will now be handled through AssignmentLocation entities
      // Any location information in the assignment will be processed through locationService

      // Log creation with description
      if (created_assignment.getDescription() != null) {
        log.debug(
            "Created assignment with description (length: {}): {}",
            created_assignment.getDescription().length(),
            created_assignment.getExternalId());
      } else {
        log.warn(
            "Created assignment but description is null: {}", created_assignment.getExternalId());
      }
    }
  }

  /**
   * Update job progress in the database
   *
   * @param jobId The job ID
   * @param foundCount Total assignments found
   * @param createdCount Assignments created
   * @param updatedCount Assignments updated
   * @param processedIds IDs of processed assignments
   */
  @Transactional
  protected void updateJobProgress(
      String jobId, int foundCount, int createdCount, int updatedCount, List<String> processedIds) {
    crawlerJobRepository
        .findById(jobId)
        .ifPresent(
            updatedJob -> {
              updatedJob.setAssignmentsFound(foundCount);
              updatedJob.setAssignmentsCreated(createdCount);
              updatedJob.setAssignmentsUpdated(updatedCount);
              updatedJob.setProcessedAssignmentIds(String.join(",", processedIds));
              crawlerJobRepository.save(updatedJob);
            });
  }

  /**
   * Update job status in the database
   *
   * @param jobId The job ID
   * @param result The job result
   * @param errorMessage Any error message (if applicable)
   */
  @Transactional
  protected void updateJobStatus(String jobId, StatusType status, String errorMessage) {
    crawlerJobRepository
        .findById(jobId)
        .ifPresent(
            job -> {
              job.setStatus(status);
              job.setErrorMessage(errorMessage);

              // We need to check the status name, not the enum
              if (!"RUNNING".equals(status.getName()) && !"SCHEDULED".equals(status.getName())) {
                job.setEndTime(LocalDateTime.now());
              }

              crawlerJobRepository.save(job);
            });
  }

  /**
   * Update assignment fields from a new assignment
   *
   * @param existing The existing assignment to update
   * @param newAssignment The new assignment with updated fields
   */
  /**
   * Get a StatusType by name with entity type CRAWLER_JOB
   *
   * @param name The name of the status type
   * @return The StatusType entity
   * @throws IllegalStateException if the status type doesn't exist
   */
  private StatusType getStatusType(String name) {
    return statusTypeRepository
        .findByNameAndEntityType(name, "CRAWLER_JOB")
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "StatusType with name " + name + " and entityType CRAWLER_JOB not found"));
  }

  private void updateAssignmentFields(Assignment existing, Assignment newAssignment) {
    // Update all relevant fields
    existing.setTitle(newAssignment.getTitle());
    existing.setDescription(newAssignment.getDescription());
    existing.setCompanyName(newAssignment.getCompanyName());
    existing.setRemotePercentage(newAssignment.getRemotePercentage());
    existing.setDurationMonths(newAssignment.getDurationMonths());
    existing.setStartDate(newAssignment.getStartDate());
    existing.setHourlyRateMin(newAssignment.getHourlyRateMin());
    existing.setHourlyRateMax(newAssignment.getHourlyRateMax());
    existing.setCurrency(newAssignment.getCurrency());
    existing.setHoursPerWeek(newAssignment.getHoursPerWeek());
    existing.setApplicationDeadline(newAssignment.getApplicationDeadline());
    existing.setApplicationUrl(newAssignment.getApplicationUrl());
    existing.setActive(newAssignment.isActive());
    existing.setStatus(newAssignment.getStatus());

    // Note: Skills are now managed through AssignmentSkill entities
    // We'll need to handle this in the AssignmentService

    // Location normalization will now be handled through AssignmentLocation entities
    // Any location information needs to be processed through specific AssignmentLocation entities
  }
}
