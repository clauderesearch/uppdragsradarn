package com.uppdragsradarn.infrastructure.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uppdragsradarn.application.service.LocationService;
import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.AssignmentLocation;
import com.uppdragsradarn.domain.repository.AssignmentLocationRepository;
import com.uppdragsradarn.domain.repository.AssignmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for batch processing of location normalization tasks */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationNormalizationBatchService {

  private final AssignmentRepository assignmentRepository;
  private final AssignmentLocationRepository assignmentLocationRepository;
  private final LocationService locationService;

  private static final int BATCH_SIZE = 100;

  /**
   * Normalize locations for all active assignments
   *
   * @return The number of assignments processed
   */
  @Async
  public void normalizeAllLocations() {
    log.info("Starting batch normalization of all assignment locations");

    int totalProcessed = 0;
    int page = 0;
    int totalNormalized = 0;

    while (true) {
      Pageable pageable = PageRequest.of(page, BATCH_SIZE);
      Page<Assignment> assignments =
          assignmentRepository.findByActiveTrueAndNeedsManualReviewFalse(pageable);

      if (!assignments.hasContent()) {
        break;
      }

      int normalizedInBatch = processAssignmentBatch(assignments.getContent());
      totalNormalized += normalizedInBatch;
      totalProcessed += assignments.getContent().size();

      log.info(
          "Processed batch {} with {} assignments, normalized {}",
          page,
          assignments.getContent().size(),
          normalizedInBatch);

      if (!assignments.hasNext()) {
        break;
      }

      page++;
    }

    log.info(
        "Completed batch normalization. Total processed: {}, Total normalized: {}",
        totalProcessed,
        totalNormalized);
  }

  /**
   * Process a batch of assignments for location normalization
   *
   * @param assignments List of assignments to process
   * @return Number of assignments normalized
   */
  @Transactional
  protected int processAssignmentBatch(List<Assignment> assignments) {
    AtomicInteger normalized = new AtomicInteger(0);

    for (Assignment assignment : assignments) {
      try {
        // Get existing assignment locations
        List<AssignmentLocation> existingLocations =
            assignmentLocationRepository.findByAssignment(assignment);

        // If assignment has no location associations, try to find location information from
        // original data
        if (existingLocations.isEmpty()) {
          // Get primary location text from assignment metadata if available
          String locationText = getPrimaryLocationText(assignment);

          if (locationText != null && !locationText.isEmpty()) {
            // Get source provider name if available
            String providerName =
                assignment.getSource() != null ? assignment.getSource().getName() : null;

            // Process the location
            locationService.processAssignmentLocation(assignment, locationText, providerName);

            // Mark assignment as updated
            assignment.setUpdatedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);

            normalized.incrementAndGet();
          }
        }
      } catch (Exception e) {
        log.error(
            "Error normalizing location for assignment {}: {}",
            assignment.getId(),
            e.getMessage(),
            e);
      }
    }

    return normalized.get();
  }

  /**
   * Helper method to get the primary location text from assignment metadata This is a transitional
   * method to handle the migration from direct location field to AssignmentLocation
   *
   * @param assignment The assignment to extract location from
   * @return The location text if available, or null
   */
  private String getPrimaryLocationText(Assignment assignment) {
    // Try to get from metadata or any other source of original location information
    // This will depend on how the location data was stored before migration
    try {
      // For example, assignment might have metadata with original location
      // or a transitional field that maintains this information
      return assignment.getOriginalLocationText();
    } catch (Exception e) {
      log.debug("Could not extract location text from assignment: {}", assignment.getId());
      return null;
    }
  }
}
