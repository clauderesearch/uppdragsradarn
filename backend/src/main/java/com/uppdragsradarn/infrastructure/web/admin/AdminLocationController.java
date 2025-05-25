package com.uppdragsradarn.infrastructure.web.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uppdragsradarn.application.dto.LocationDto;
import com.uppdragsradarn.application.service.LocationService;
import com.uppdragsradarn.domain.model.Location;
import com.uppdragsradarn.infrastructure.service.LocationNormalizationBatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Admin controller for location management */
@RestController
@RequestMapping("/api/admin/locations")
@RequiredArgsConstructor
@Slf4j
public class AdminLocationController {

  private final LocationService locationService;
  private final LocationNormalizationBatchService batchService;

  /** Search for locations by term */
  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
  public List<LocationDto> searchLocations(
      @RequestParam String query, @RequestParam(defaultValue = "20") int limit) {
    log.info("Searching for locations with query: {}", query);
    List<Location> locations = locationService.searchLocations(query, limit);
    return locations.stream().map(LocationDto::fromEntity).toList();
  }

  /** Get location by ID */
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
  public ResponseEntity<LocationDto> getLocationById(@PathVariable UUID id) {
    log.info("Getting location with ID: {}", id);
    Location location = locationService.getLocationById(id);
    return ResponseEntity.ok(LocationDto.fromEntity(location));
  }

  /** Create or update a location */
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
  public ResponseEntity<LocationDto> createOrUpdateLocation(@RequestBody LocationDto locationDto) {
    log.info("Creating or updating location: {}", locationDto);

    Location location;
    if (locationDto.getId() != null) {
      // Update existing location
      location = locationService.getLocationById(locationDto.getId());

      location.setCity(locationDto.getCity());
      location.setRegion(locationDto.getRegion());
      location.setCountryCode(locationDto.getCountryCode());
      location.setCountryName(locationDto.getCountryName());
      // Latitude/longitude are deprecated and no longer stored
      if (locationDto.getLatitude() != null || locationDto.getLongitude() != null) {
        log.warn(
            "Setting latitude/longitude values which are deprecated and no longer stored in the database");
      }
      location.setLatitude(locationDto.getLatitude());
      location.setLongitude(locationDto.getLongitude());
      location.setPopulation(locationDto.getPopulation());
      location.setGeonameId(locationDto.getGeonameId());
      location.setRemoteFriendly(locationDto.isRemoteFriendly());

      location = locationService.updateLocation(location);
    } else {
      // Create new location
      location =
          locationService.findOrCreateLocation(
              locationDto.getCity(),
              locationDto.getRegion(),
              locationDto.getCountryCode(),
              locationDto.getCountryName());

      // Update additional fields if provided
      if (locationDto.getLatitude() != null
          || locationDto.getLongitude() != null
          || locationDto.getPopulation() != null
          || locationDto.getGeonameId() != null) {

        // Latitude/longitude are deprecated and no longer stored
        if (locationDto.getLatitude() != null || locationDto.getLongitude() != null) {
          log.warn(
              "Setting latitude/longitude values which are deprecated and no longer stored in the database");
        }
        location.setLatitude(locationDto.getLatitude());
        location.setLongitude(locationDto.getLongitude());
        location.setPopulation(locationDto.getPopulation());
        location.setGeonameId(locationDto.getGeonameId());
        location.setRemoteFriendly(locationDto.isRemoteFriendly());

        location = locationService.updateLocation(location);
      }
    }

    return ResponseEntity.ok(LocationDto.fromEntity(location));
  }

  /** Normalize a location string for testing */
  @GetMapping("/normalize")
  @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
  public ResponseEntity<LocationDto> normalizeLocationText(
      @RequestParam String locationText, @RequestParam(required = false) String provider) {

    log.info("Normalizing location text: '{}' with provider: '{}'", locationText, provider);

    Optional<Location> normalizedLocation =
        locationService.normalizeLocation(locationText, provider);

    if (normalizedLocation.isPresent()) {
      return ResponseEntity.ok(LocationDto.fromEntity(normalizedLocation.get()));
    } else {
      return ResponseEntity.noContent().build();
    }
  }

  /** Start batch normalization of all assignment locations */
  @PostMapping("/normalize-all")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<String> startBatchNormalization() {
    log.info("Starting batch normalization of all locations");
    batchService.normalizeAllLocations();
    return ResponseEntity.accepted().body("Batch normalization started");
  }
}
