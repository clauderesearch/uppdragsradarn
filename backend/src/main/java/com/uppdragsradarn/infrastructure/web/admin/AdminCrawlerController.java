package com.uppdragsradarn.infrastructure.web.admin;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.uppdragsradarn.application.service.CrawlerService;
import com.uppdragsradarn.domain.model.CrawlerJobExecution;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controller for managing crawler operations */
@RestController
@RequestMapping("/api/admin/crawler")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Crawler", description = "Administrative operations for managing web crawlers")
public class AdminCrawlerController {

  private final CrawlerService crawlerService;

  /**
   * Start a crawler job for a specific source
   *
   * @param sourceId The source ID to crawl
   * @return The job ID
   */
  @Operation(
      summary = "Start a crawler job for a specific source",
      description =
          "Triggers a new crawler job for the specified source ID. Requires admin privileges.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Crawler job started successfully"),
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid credentials"),
    @ApiResponse(responseCode = "403", description = "Forbidden - missing required role"),
    @ApiResponse(responseCode = "500", description = "Internal server error starting job")
  })
  @PostMapping("/jobs/source/{sourceId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> startCrawlerJob(
      @Parameter(description = "Source ID to crawl", required = true) @PathVariable UUID sourceId) {
    log.info("Starting crawler job for source ID: {}", sourceId);
    try {
      String jobId = crawlerService.startCrawlerJob(sourceId).getId();
      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "jobId",
              jobId.toString(),
              "message",
              "Crawler job started for source ID: " + sourceId,
              "timestamp",
              LocalDateTime.now().toString()));
    } catch (Exception e) {
      log.error("Error starting crawler job for source ID: {}", sourceId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "success",
                  false,
                  "message",
                  "Error starting crawler job: " + e.getMessage(),
                  "timestamp",
                  LocalDateTime.now().toString()));
    }
  }

  /**
   * Get status of a specific crawler job
   *
   * @param jobId The job ID to get status for
   * @return The job status
   */
  @Operation(
      summary = "Get status of a crawler job",
      description = "Returns the current status of a specific crawler job by ID.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved job status",
        content = @Content(schema = @Schema(implementation = CrawlerJobExecution.class))),
    @ApiResponse(responseCode = "404", description = "Job not found")
  })
  @GetMapping("/jobs/{jobId}")
  public ResponseEntity<CrawlerJobExecution> getCrawlerJobStatus(
      @Parameter(description = "Job ID to get status for", required = true) @PathVariable
          String jobId) {
    log.info("Getting status for crawler job ID: {}", jobId);
    return crawlerService
        .getCrawlerJobStatus(jobId)
        .map(status -> ResponseEntity.ok().body(status))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Start all scheduled crawler jobs
   *
   * @return Response with status
   */
  @PostMapping("/jobs/scheduled")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> startScheduledCrawlerJobs() {
    log.info("Starting scheduled crawler jobs");
    try {
      crawlerService.startScheduledCrawlerJobs();
      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "message",
              "Scheduled crawler jobs started",
              "timestamp",
              LocalDateTime.now().toString()));
    } catch (Exception e) {
      log.error("Error starting scheduled crawler jobs", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "success",
                  false,
                  "message",
                  "Error starting scheduled crawler jobs: " + e.getMessage(),
                  "timestamp",
                  LocalDateTime.now().toString()));
    }
  }

  /**
   * Public endpoint to trigger crawler jobs without authentication This is useful for development
   * and testing
   *
   * @return Response with status
   */
  @PostMapping("/public/run-now")
  public ResponseEntity<Map<String, Object>> runCrawlersNow() {
    log.info("Triggering crawler jobs via public endpoint");
    try {
      crawlerService.startScheduledCrawlerJobs();
      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "message",
              "Crawler jobs started successfully",
              "timestamp",
              LocalDateTime.now().toString()));
    } catch (Exception e) {
      log.error("Error starting crawler jobs via public endpoint", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "success",
                  false,
                  "message",
                  "Error starting crawler jobs: " + e.getMessage(),
                  "timestamp",
                  LocalDateTime.now().toString()));
    }
  }

  /**
   * Cancel a running crawler job
   *
   * @param jobId The job ID to cancel
   * @return Response with status
   */
  @DeleteMapping("/jobs/{jobId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> cancelCrawlerJob(@PathVariable String jobId) {
    log.info("Cancelling crawler job ID: {}", jobId);
    try {
      boolean cancelled = crawlerService.cancelCrawlerJob(jobId);
      if (cancelled) {
        return ResponseEntity.ok(
            Map.of(
                "success",
                true,
                "message",
                "Crawler job cancelled successfully",
                "timestamp",
                LocalDateTime.now().toString()));
      } else {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                Map.of(
                    "success",
                    false,
                    "message",
                    "Crawler job not found or already completed",
                    "timestamp",
                    LocalDateTime.now().toString()));
      }
    } catch (Exception e) {
      log.error("Error cancelling crawler job ID: {}", jobId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "success",
                  false,
                  "message",
                  "Error cancelling crawler job: " + e.getMessage(),
                  "timestamp",
                  LocalDateTime.now().toString()));
    }
  }
}
