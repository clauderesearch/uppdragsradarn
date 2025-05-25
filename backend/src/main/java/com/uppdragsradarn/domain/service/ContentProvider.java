package com.uppdragsradarn.domain.service;

import java.util.List;

import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.CrawlerException;
import com.uppdragsradarn.domain.model.Source;

/**
 * A content provider fetches and parses assignments from a source. This interface combines the
 * functionality of fetching and parsing in one place.
 */
public interface ContentProvider {

  /**
   * Fetch and parse assignments from a source
   *
   * @param source The source to fetch assignments from
   * @return List of parsed assignments
   * @throws CrawlerException if there's an error fetching or parsing assignments
   */
  List<Assignment> getAssignments(Source source) throws CrawlerException;

  /**
   * Check if this provider supports the given source
   *
   * @param source The source to check
   * @return true if this provider supports the source, false otherwise
   */
  boolean supports(Source source);

  /**
   * Get the name of this provider
   *
   * @return The provider name
   */
  String getName();
}
