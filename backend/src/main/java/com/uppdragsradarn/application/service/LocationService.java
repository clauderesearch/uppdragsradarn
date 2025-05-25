package com.uppdragsradarn.application.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uppdragsradarn.domain.exception.ResourceNotFoundException;
import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.AssignmentLocation;
import com.uppdragsradarn.domain.model.Location;
import com.uppdragsradarn.domain.model.LocationAlias;
import com.uppdragsradarn.domain.repository.AssignmentLocationRepository;
import com.uppdragsradarn.domain.repository.LocationAliasRepository;
import com.uppdragsradarn.domain.repository.LocationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for managing location data and normalization. */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

  private final LocationRepository locationRepository;
  private final LocationAliasRepository locationAliasRepository;
  private final AssignmentLocationRepository assignmentLocationRepository;

  // Constants for location processing
  private static final Pattern REMOTE_PATTERN =
      Pattern.compile(
          "\\b(remote|distans|på\\s+distans|hemifr[åa]n|hemifrån|remote\\s+work|work\\s+from\\s+home|remote-?based)\\b",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern REMOTE_PERCENTAGE_PATTERN =
      Pattern.compile(
          "\\b(\\d{1,3})\\s*%\\s*(remote|distans|på\\s+distans|hemifr[åa]n|remote\\s+work|work\\s+from\\s+home)",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern SPLIT_PATTERN = Pattern.compile("[,;/]");

  private static final Set<String> COUNTRY_NAMES =
      new HashSet<>(
          Arrays.asList(
              "sweden",
              "sverige",
              "swedish",
              "sverige",
              "svensk",
              "svenska",
              "norway",
              "norge",
              "norwegian",
              "norsk",
              "denmark",
              "danmark",
              "danish",
              "dansk",
              "finland",
              "finnish",
              "suomi",
              "suomalainen"));

  private static final Set<String> REMOTE_INDICATORS =
      new HashSet<>(
          Arrays.asList(
              "remote",
              "distans",
              "på distans",
              "hemifrån",
              "remote work",
              "work from home",
              "remote-based",
              "remote based",
              "100% remote",
              "fully remote"));

  /**
   * Find a location by its ID
   *
   * @param id The location ID to look up
   * @return The location if found
   */
  @Transactional(readOnly = true)
  public Location getLocationById(UUID id) {
    return locationRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + id));
  }

  /**
   * Find a location by GeoName ID
   *
   * @param geonameId The GeoName database ID
   * @return The location if found
   */
  @Transactional(readOnly = true)
  public Optional<Location> findByGeonameId(Long geonameId) {
    return locationRepository.findByGeonameIdAndActiveTrue(geonameId);
  }

  /**
   * Find a location by city and country code
   *
   * @param city The city name
   * @param countryCode The ISO 3166-1 alpha-2 country code
   * @return The location if found
   */
  @Transactional(readOnly = true)
  public Optional<Location> findByCityAndCountry(String city, String countryCode) {
    return locationRepository.findByCityAndCountryCode(city, countryCode);
  }

  /**
   * Find a location by city, region, and country code
   *
   * @param city The city name
   * @param region The region/state/province
   * @param countryCode The ISO 3166-1 alpha-2 country code
   * @return The location if found
   */
  @Transactional(readOnly = true)
  public Optional<Location> findByCityRegionAndCountry(
      String city, String region, String countryCode) {
    return locationRepository.findByCityAndRegionAndCountryCode(city, region, countryCode);
  }

  /**
   * Create a new location
   *
   * @param location The location to create
   * @return The created location
   */
  @Transactional
  public Location createLocation(Location location) {
    if (location.getCreatedAt() == null) {
      location.setCreatedAt(LocalDateTime.now());
    }
    location.setUpdatedAt(LocalDateTime.now());

    return locationRepository.save(location);
  }

  /**
   * Update an existing location
   *
   * @param location The location to update
   * @return The updated location
   */
  @Transactional
  public Location updateLocation(Location location) {
    location.setUpdatedAt(LocalDateTime.now());
    return locationRepository.save(location);
  }

  /**
   * Find or create a location
   *
   * @param city The city name
   * @param region The region/state/province (optional)
   * @param countryCode The ISO 3166-1 alpha-2 country code
   * @param countryName The country name
   * @return The existing or newly created location
   */
  @Transactional
  public Location findOrCreateLocation(
      String city, String region, String countryCode, String countryName) {
    if (city == null
        || city.trim().isEmpty()
        || countryCode == null
        || countryCode.trim().isEmpty()) {
      throw new IllegalArgumentException("City and country code are required");
    }

    String trimmedCity = city.trim();
    String trimmedRegion = region != null ? region.trim() : null;
    String trimmedCountryCode = countryCode.trim();

    // Try to find existing location
    Optional<Location> existingLocation;
    if (trimmedRegion != null && !trimmedRegion.isEmpty()) {
      existingLocation = findByCityRegionAndCountry(trimmedCity, trimmedRegion, trimmedCountryCode);
    } else {
      existingLocation = findByCityAndCountry(trimmedCity, trimmedCountryCode);
    }

    // Return existing location if found
    if (existingLocation.isPresent()) {
      return existingLocation.get();
    }

    // Create new location
    Location newLocation =
        Location.builder()
            .city(trimmedCity)
            .region(trimmedRegion)
            .countryCode(trimmedCountryCode)
            .countryName(countryName != null ? countryName.trim() : null)
            .active(true)
            .build();

    return createLocation(newLocation);
  }

  /**
   * Search for locations by term (for autocomplete)
   *
   * @param searchTerm The search text
   * @param limit Maximum number of results to return
   * @return List of matching locations
   */
  @Transactional(readOnly = true)
  public List<Location> searchLocations(String searchTerm, int limit) {
    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      return Collections.emptyList();
    }

    return locationRepository.searchLocations(searchTerm.trim(), limit);
  }

  /**
   * Find locations near coordinates
   *
   * @param latitude The latitude
   * @param longitude The longitude
   * @param radiusKm Radius in kilometers
   * @return List of locations within radius
   */
  @Transactional(readOnly = true)
  public List<Location> findLocationsNearby(double latitude, double longitude, double radiusKm) {
    // Proximity search functionality removed as latitude/longitude data is no longer stored
    log.warn("Location proximity search called but this feature is no longer available");
    return Collections.emptyList();
  }

  /**
   * Add an alias for a location
   *
   * @param locationId The canonical location ID
   * @param aliasText The alias text
   * @param sourceText The original source text (optional)
   * @param sourceProvider The source provider name (optional)
   * @param matchConfidence Confidence score for the match (0.0-1.0)
   * @param manualMatch Whether this was manually matched
   * @return The created alias
   */
  @Transactional
  public LocationAlias addLocationAlias(
      UUID locationId,
      String aliasText,
      String sourceText,
      String sourceProvider,
      Float matchConfidence,
      boolean manualMatch) {

    Location location = getLocationById(locationId);

    LocationAlias alias =
        LocationAlias.builder()
            .aliasText(aliasText.trim())
            .sourceText(sourceText != null ? sourceText.trim() : null)
            .sourceProvider(sourceProvider)
            .matchConfidence(matchConfidence)
            .manualMatch(manualMatch)
            .location(location)
            .isActive(true)
            .build();

    return locationAliasRepository.save(alias);
  }

  /**
   * Find location by alias text
   *
   * @param aliasText The alias text to look up
   * @return Optional containing the canonical location if found
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "locationAlias", key = "#aliasText")
  public Optional<Location> findLocationByAlias(String aliasText) {
    if (aliasText == null || aliasText.trim().isEmpty()) {
      return Optional.empty();
    }

    String trimmedAlias = aliasText.trim();

    // Try exact match first
    Optional<LocationAlias> alias =
        locationAliasRepository.findByAliasTextAndIsActiveTrue(trimmedAlias);
    if (alias.isPresent()) {
      return Optional.of(alias.get().getLocation());
    }

    // Try with similar aliases
    List<LocationAlias> similarAliases =
        locationAliasRepository.findSimilarAliases(trimmedAlias, 1);
    if (!similarAliases.isEmpty()
        && similarAliases.get(0).getMatchConfidence() != null
        && similarAliases.get(0).getMatchConfidence() > 0.7f) {
      return Optional.of(similarAliases.get(0).getLocation());
    }

    return Optional.empty();
  }

  /**
   * Normalize a raw location string to a canonical location
   *
   * @param rawLocation The raw location string to normalize
   * @param sourceProvider The source provider name (optional)
   * @return Optional containing the canonical location if found
   */
  @Transactional
  public Optional<Location> normalizeLocation(String rawLocation, String sourceProvider) {
    if (rawLocation == null || rawLocation.trim().isEmpty()) {
      return Optional.empty();
    }

    String trimmedLocation = rawLocation.trim();

    // Check if we already know this location from this provider
    if (sourceProvider != null) {
      Optional<LocationAlias> existingAlias =
          locationAliasRepository.findBySourceTextAndSourceProviderAndIsActiveTrue(
              trimmedLocation, sourceProvider);
      if (existingAlias.isPresent()) {
        return Optional.of(existingAlias.get().getLocation());
      }
    }

    // Check for exact match with alias
    Optional<Location> aliasMatch = findLocationByAlias(trimmedLocation);
    if (aliasMatch.isPresent()) {
      return aliasMatch;
    }

    // If it has multiple locations separated by delimiter, process the first one
    if (SPLIT_PATTERN.matcher(trimmedLocation).find()) {
      String[] parts = SPLIT_PATTERN.split(trimmedLocation);
      if (parts.length > 0) {
        String primaryPart = parts[0].trim();

        // Try to normalize the primary part
        Optional<Location> firstMatch = normalizeLocationPart(primaryPart);
        if (firstMatch.isPresent()) {
          // Add an alias for the full raw string
          addLocationAlias(
              firstMatch.get().getId(),
              trimmedLocation,
              trimmedLocation,
              sourceProvider,
              0.8f,
              false);

          return firstMatch;
        }
      }
    }

    // If no delimiter, try to normalize directly
    return normalizeLocationPart(trimmedLocation);
  }

  /** Helper method to normalize a single location part */
  private Optional<Location> normalizeLocationPart(String locationPart) {
    if (locationPart == null || locationPart.trim().isEmpty()) {
      return Optional.empty();
    }

    String trimmedPart = locationPart.trim();

    // Default country if none specified
    String countryCode = "SE";
    String countryName = "Sweden";

    // Remote location handling
    if (isRemoteLocation(trimmedPart)) {
      // Find or create the "Remote" location
      return findByCityAndCountry("Remote", countryCode);
    }

    // Try simple city match
    List<Location> possibleMatches =
        locationRepository.findByCityContainingIgnoreCaseAndActiveTrue(trimmedPart);
    if (!possibleMatches.isEmpty()) {
      // Sort by population (descending) and pick the first match
      possibleMatches.sort(
          (a, b) -> {
            if (a.getPopulation() == null && b.getPopulation() == null) return 0;
            if (a.getPopulation() == null) return 1;
            if (b.getPopulation() == null) return -1;
            return b.getPopulation().compareTo(a.getPopulation());
          });

      return Optional.of(possibleMatches.get(0));
    }

    // No match found
    return Optional.empty();
  }

  /**
   * Add an assignment-location association
   *
   * @param assignment The assignment
   * @param location The location
   * @param originalText The original location text
   * @param primary Whether this is the primary location
   * @param remote Whether this is a remote position
   * @param remotePercentage Percentage of remote work
   * @return The created association
   */
  @Transactional
  public AssignmentLocation addAssignmentLocation(
      Assignment assignment,
      Location location,
      String originalText,
      boolean primary,
      boolean remote,
      Integer remotePercentage) {

    AssignmentLocation assignmentLocation =
        AssignmentLocation.builder()
            .assignment(assignment)
            .location(location)
            .originalText(originalText)
            .primary(primary)
            .remote(remote)
            .remotePercentage(remotePercentage)
            .build();

    return assignmentLocationRepository.save(assignmentLocation);
  }

  /**
   * Get all locations for an assignment
   *
   * @param assignment The assignment
   * @return List of assignment-location relations
   */
  @Transactional(readOnly = true)
  public List<AssignmentLocation> getLocationsForAssignment(Assignment assignment) {
    return assignmentLocationRepository.findByAssignment(assignment);
  }

  /**
   * Get the primary location for an assignment
   *
   * @param assignment The assignment
   * @return Optional containing the primary location if found
   */
  @Transactional(readOnly = true)
  public Optional<AssignmentLocation> getPrimaryLocationForAssignment(Assignment assignment) {
    return assignmentLocationRepository.findByAssignmentAndPrimaryTrue(assignment);
  }

  /**
   * Process and normalize assignment locations from raw text
   *
   * @param assignment The assignment to process
   * @param rawLocationText The raw location text
   * @param sourceProvider The source provider name
   */
  @Transactional
  public void processAssignmentLocation(
      Assignment assignment, String rawLocationText, String sourceProvider) {
    if (rawLocationText == null || rawLocationText.trim().isEmpty()) {
      return;
    }

    try {
      String trimmedLocation = rawLocationText.trim();
      boolean isRemote = isRemoteLocation(trimmedLocation);
      Integer remotePercentage = extractRemotePercentage(trimmedLocation);

      // Normalize location
      Optional<Location> normalizedLocation = normalizeLocation(trimmedLocation, sourceProvider);

      // If we couldn't normalize the location but it's remote, use the "Remote" location
      if (normalizedLocation.isEmpty() && isRemote) {
        normalizedLocation = findByCityAndCountry("Remote", "SE");
      }

      // If still can't find a match and we don't have fallback data yet, create a default for
      // Stockholm
      if (normalizedLocation.isEmpty()) {
        // Try to find a common city like Stockholm as fallback
        normalizedLocation = findByCityAndCountry("Stockholm", "SE");

        // If even that fails, log a warning and return without creating an association
        if (normalizedLocation.isEmpty()) {
          log.warn(
              "Could not normalize location for assignment {} and no fallback data available: {}",
              assignment.getId(),
              trimmedLocation);
          return;
        }
      }

      Location location = normalizedLocation.get();

      try {
        // Check if relationship already exists
        Optional<AssignmentLocation> existingRelation =
            assignmentLocationRepository.findByAssignmentAndLocation(assignment, location);

        if (existingRelation.isEmpty()) {
          // Create new association
          addAssignmentLocation(
              assignment,
              location,
              trimmedLocation,
              true, // Primary location (since it's the first/only one we're processing)
              isRemote,
              remotePercentage);
        } else {
          // Update existing relationship
          AssignmentLocation relation = existingRelation.get();
          relation.setRemote(isRemote);
          relation.setRemotePercentage(remotePercentage);
          relation.setOriginalText(trimmedLocation);

          assignmentLocationRepository.save(relation);
        }
      } catch (Exception e) {
        log.error(
            "Error processing assignment-location relationship for assignment {}: {}",
            assignment.getId(),
            e.getMessage(),
            e);
      }
    } catch (Exception e) {
      log.error(
          "Error processing location for assignment {}: {}", assignment.getId(), e.getMessage(), e);
    }
  }

  /**
   * Check if a location string indicates remote work
   *
   * @param locationText The location text to check
   * @return True if the text indicates remote work
   */
  @Transactional(readOnly = true)
  public boolean isRemoteLocation(String locationText) {
    if (locationText == null || locationText.trim().isEmpty()) {
      return false;
    }

    String lowerCaseText = locationText.toLowerCase();

    // Check for direct matches with remote indicators
    for (String indicator : REMOTE_INDICATORS) {
      if (lowerCaseText.contains(indicator.toLowerCase())) {
        return true;
      }
    }

    // Use regex pattern for more complex matches
    return REMOTE_PATTERN.matcher(locationText).find();
  }

  /**
   * Extract remote work percentage from text if available
   *
   * @param locationText The location text to check
   * @return The remote percentage or null if not found
   */
  @Transactional(readOnly = true)
  public Integer extractRemotePercentage(String locationText) {
    if (locationText == null || locationText.trim().isEmpty()) {
      return null;
    }

    Matcher matcher = REMOTE_PERCENTAGE_PATTERN.matcher(locationText);
    if (matcher.find()) {
      try {
        int percentage = Integer.parseInt(matcher.group(1));
        // Validate percentage is between 0 and 100
        if (percentage >= 0 && percentage <= 100) {
          return percentage;
        }
      } catch (NumberFormatException e) {
        log.debug("Failed to parse remote percentage from: {}", locationText);
      }
    }

    // If text indicates remote but no percentage, assume 100%
    if (isRemoteLocation(locationText)) {
      return 100;
    }

    return null;
  }
}
