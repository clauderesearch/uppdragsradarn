package com.uppdragsradarn.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Join table for the many-to-many relationship between assignments and locations. An assignment can
 * have multiple locations (e.g., distributed teams) and locations can have multiple assignments.
 */
@Entity
@Table(name = "assignment_locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"assignment", "location"})
public class AssignmentLocation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "assignment_id", nullable = false)
  private Assignment assignment;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "location_id", nullable = false)
  private Location location;

  @Column(name = "original_text")
  private String originalText;

  @Column(name = "is_remote")
  private boolean remote;

  @Column(name = "remote_percentage")
  private Integer remotePercentage;

  @Column(name = "is_primary")
  private boolean primary;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
