package com.uppdragsradarn.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uppdragsradarn.domain.model.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

  /** Find location by city and country code */
  Optional<Location> findByCityAndCountryCode(String city, String countryCode);

  /** Find location by city, region and country code */
  Optional<Location> findByCityAndRegionAndCountryCode(
      String city, String region, String countryCode);

  /** Find locations by city name (case insensitive partial match) */
  List<Location> findByCityContainingIgnoreCaseAndActiveTrue(String cityPart);

  /** Find active locations by geoname ID */
  Optional<Location> findByGeonameIdAndActiveTrue(Long geonameId);

  /** Search for locations by city, region, or country (for autocomplete) */
  @Query(
      "SELECT l FROM Location l WHERE l.active = true AND "
          + "(LOWER(l.city) LIKE LOWER(CONCAT('%', :term, '%')) OR "
          + "LOWER(l.region) LIKE LOWER(CONCAT('%', :term, '%')) OR "
          + "LOWER(l.countryName) LIKE LOWER(CONCAT('%', :term, '%'))) "
          + "ORDER BY l.population DESC NULLS LAST")
  List<Location> searchLocations(@Param("term") String searchTerm, @Param("limit") int limit);

  // Location proximity search methods removed as latitude/longitude data is no longer stored

  /** Find all locations marked as remote-friendly */
  List<Location> findByRemoteFriendlyTrueAndActiveTrue();
}
