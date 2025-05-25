package com.uppdragsradarn.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.uppdragsradarn.domain.model.CrawlerJobExecution;

/** Repository for crawler job executions */
@Repository
public interface CrawlerJobRepository extends JpaRepository<CrawlerJobExecution, String> {

  /**
   * Find recent crawler jobs sorted by start time descending
   *
   * @param pageable Pagination information
   * @return List of crawler jobs
   */
  @Query("SELECT c FROM CrawlerJobExecution c ORDER BY c.startTime DESC")
  List<CrawlerJobExecution> findRecent(Pageable pageable);

  /**
   * Find crawler jobs for a specific source
   *
   * @param sourceId The source ID
   * @param pageable Pagination information
   * @return List of crawler jobs for the source
   */
  @Query(
      "SELECT c FROM CrawlerJobExecution c WHERE c.source.id = :sourceId ORDER BY c.startTime DESC")
  List<CrawlerJobExecution> findBySourceId(UUID sourceId, Pageable pageable);
}
