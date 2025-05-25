package com.uppdragsradarn.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.Source;
import com.uppdragsradarn.domain.model.User;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

  /** Find all active assignments */
  Page<Assignment> findByActiveTrueAndNeedsManualReviewFalse(Pageable pageable);

  /** Find all active assignments (without pagination) */
  List<Assignment> findByActiveTrueAndNeedsManualReviewFalse();

  /** Find an assignment by source and external ID */
  Optional<Assignment> findBySourceAndExternalId(Source source, String externalId);

  /** Find assignments by title (case insensitive) */
  Page<Assignment> findByActiveTrueAndNeedsManualReviewFalseAndTitleContainingIgnoreCase(
      String title, Pageable pageable);

  /** Find assignments by title and location (case insensitive) */
  @Query(
      "SELECT DISTINCT a FROM Assignment a LEFT JOIN a.assignmentLocations al LEFT JOIN al.location l "
          + "WHERE a.active = true AND a.needsManualReview = false "
          + "AND LOWER(a.title) LIKE LOWER(CONCAT('%', :title, '%')) "
          + "AND (LOWER(l.city) LIKE LOWER(CONCAT('%', :location, '%')) "
          + "OR LOWER(l.region) LIKE LOWER(CONCAT('%', :location, '%')) "
          + "OR LOWER(l.countryName) LIKE LOWER(CONCAT('%', :location, '%')))")
  Page<Assignment> findByTitleAndLocation(
      @Param("title") String title, @Param("location") String location, Pageable pageable);

  /** Count assignments for a specific source */
  long countBySource(Source source);

  /**
   * Find all active assignments with time gating applied for free users (assignments created more
   * than 72 hours ago)
   */
  @Query(
      "SELECT a FROM Assignment a WHERE a.active = true AND a.needsManualReview = false "
          + "AND (a.createdAt <= :cutoffDate)")
  Page<Assignment> findByActiveTrueAndCreatedAtBefore(
      @Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

  /** Find assignments by title with time gating applied for free users */
  @Query(
      "SELECT a FROM Assignment a WHERE a.active = true AND a.needsManualReview = false "
          + "AND LOWER(a.title) LIKE LOWER(CONCAT('%', :title, '%')) "
          + "AND (a.createdAt <= :cutoffDate)")
  Page<Assignment> findByActiveTrueAndTitleContainingIgnoreCaseAndCreatedAtBefore(
      @Param("title") String title,
      @Param("cutoffDate") LocalDateTime cutoffDate,
      Pageable pageable);

  /**
   * Find all active assignments visible to the user - For premium users: all active assignments
   * with full details - For free users: all active assignments, but service layer will filter
   * details for recent ones
   */
  default Page<Assignment> findVisibleAssignments(User user, Pageable pageable) {
    // Return all active assignments regardless of user type
    // Service layer will handle limiting the data for premium-only assignments
    return findByActiveTrueAndNeedsManualReviewFalse(pageable);
  }

  /** Find assignments by title visible to the user */
  default Page<Assignment> findVisibleAssignmentsByTitle(
      String title, User user, Pageable pageable) {
    // Return all active assignments that match the title regardless of user type
    // Service layer will handle limiting the data for premium-only assignments
    return findByActiveTrueAndNeedsManualReviewFalseAndTitleContainingIgnoreCase(title, pageable);
  }

  /**
   * Find assignment by ID - For premium users: return full assignment if it exists and is active -
   * For free users: return the assignment even if premium-only, service layer will handle the
   * limited data
   */
  default Optional<Assignment> findVisibleAssignmentById(UUID id, User user) {
    Optional<Assignment> assignmentOpt = findById(id);

    if (assignmentOpt.isEmpty()
        || !assignmentOpt.get().isActive()
        || assignmentOpt.get().isNeedsManualReview()) {
      return Optional.empty();
    }

    // Return the assignment regardless of user type
    // Service layer will handle limiting the data for premium-only assignments
    return assignmentOpt;
  }

  /**
   * Find assignments that need manual review
   *
   * @param pageable Pagination information
   * @return Page of assignments needing review
   */
  Page<Assignment> findByNeedsManualReviewTrue(Pageable pageable);
}
