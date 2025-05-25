package com.uppdragsradarn.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an alternative name or spelling for a location. This allows matching various forms of
 * the same location to a canonical record.
 */
@Entity
@Table(name = "location_aliases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationAlias {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "alias_text", nullable = false)
  private String aliasText;

  @Column(name = "source_text")
  private String sourceText;

  @Column(name = "source_provider")
  private String sourceProvider;

  @Column(name = "match_confidence")
  private Float matchConfidence;

  @Column(name = "manual_match")
  private boolean manualMatch;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "location_id", nullable = false)
  private Location location;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
