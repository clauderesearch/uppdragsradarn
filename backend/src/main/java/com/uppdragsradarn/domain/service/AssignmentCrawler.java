package com.uppdragsradarn.domain.service;

import java.util.List;

import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.Source;

/**
 * Interface for assignment crawlers. Each crawler implementation handles a specific type of source.
 */
public interface AssignmentCrawler {

  /**
   * Checks if this crawler supports the given source type
   *
   * @param source The source to check
   * @return true if this crawler supports the source, false otherwise
   */
  boolean supports(Source source);

  /**
   * Fetches assignments from the given source
   *
   * @param source The source to fetch assignments from
   * @return List of fetched assignments
   */
  List<Assignment> fetchAssignments(Source source);

  /**
   * Gets the name of this crawler
   *
   * @return The crawler name
   */
  String getName();
}
