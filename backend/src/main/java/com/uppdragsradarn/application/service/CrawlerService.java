package com.uppdragsradarn.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.uppdragsradarn.domain.model.CrawlerJobExecution;

/** Service interface for managing crawler operations */
public interface CrawlerService {

  /**
   * Start a crawler job for a specific source
   *
   * @param sourceId The ID of the source to crawl
   * @return Details of the initiated job
   */
  CrawlerJobExecution startCrawlerJob(UUID sourceId);

  /**
   * Get the status of a specific crawler job
   *
   * @param jobId The ID of the job
   * @return The current job status
   */
  Optional<CrawlerJobExecution> getCrawlerJobStatus(String jobId);

  /**
   * Get all recent crawler job executions
   *
   * @param limit Maximum number of jobs to return
   * @return List of recent job executions
   */
  List<CrawlerJobExecution> getRecentCrawlerJobs(int limit);

  /**
   * Get recent crawler job executions for a specific source
   *
   * @param sourceId The source ID
   * @param limit Maximum number of jobs to return
   * @return List of recent job executions for the source
   */
  List<CrawlerJobExecution> getSourceCrawlerJobs(UUID sourceId, int limit);

  /** Start scheduled crawler jobs for all active sources */
  void startScheduledCrawlerJobs();

  /**
   * Cancel a running crawler job
   *
   * @param jobId The ID of the job to cancel
   * @return true if the job was cancelled, false otherwise
   */
  boolean cancelCrawlerJob(String jobId);
}
