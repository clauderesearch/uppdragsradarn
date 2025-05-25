package com.uppdragsradarn.application.dto;

import java.util.UUID;

import com.uppdragsradarn.domain.model.Location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Data Transfer Object for Location information */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

  private UUID id;
  private String city;
  private String region;
  private String countryCode;
  private String countryName;

  /**
   * @deprecated Latitude/longitude data is no longer stored in the database
   */
  @Deprecated private Double latitude;

  /**
   * @deprecated Latitude/longitude data is no longer stored in the database
   */
  @Deprecated private Double longitude;

  private Integer population;
  private Long geonameId;
  private boolean remoteFriendly;
  private String formattedName;

  /**
   * Convert entity to DTO
   *
   * @param location Location entity
   * @return LocationDto
   */
  public static LocationDto fromEntity(Location location) {
    if (location == null) {
      return null;
    }

    return LocationDto.builder()
        .id(location.getId())
        .city(location.getCity())
        .region(location.getRegion())
        .countryCode(location.getCountryCode())
        .countryName(location.getCountryName())
        .latitude(location.getLatitude())
        .longitude(location.getLongitude())
        .population(location.getPopulation())
        .geonameId(location.getGeonameId())
        .remoteFriendly(location.isRemoteFriendly())
        .formattedName(location.getFormattedName())
        .build();
  }
}
