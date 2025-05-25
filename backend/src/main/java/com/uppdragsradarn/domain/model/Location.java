package com.uppdragsradarn.domain.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a canonical location that has been normalized. Used to standardize location names
 * across different job listings.
 */
@Entity
@Table(
    name = "locations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"city", "region", "country_code"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String city;

  @Column private String region;

  @Column(name = "country_code", nullable = false, length = 2, columnDefinition = "VARCHAR(2)")
  private String countryCode; // ISO 3166-1 alpha-2 code

  @Column(name = "country_name")
  private String countryName;

  @Column private Double latitude;

  @Column private Double longitude;

  @Column(name = "population")
  private Integer population;

  @Column(name = "geoname_id")
  private Long geonameId;

  @ManyToOne
  @JoinColumn(name = "location_type_id")
  private LocationType locationType;

  @Column(name = "is_remote_friendly")
  private boolean remoteFriendly;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<LocationAlias> aliases = new HashSet<>();

  /**
   * Returns a formatted string representation of this location. Format: City, Country (or City,
   * Region, Country if region is present)
   */
  @Transient
  public String getFormattedName() {
    if (region != null && !region.isEmpty()) {
      return String.format("%s, %s, %s", city, region, countryName);
    } else {
      return String.format("%s, %s", city, countryName);
    }
  }
}
