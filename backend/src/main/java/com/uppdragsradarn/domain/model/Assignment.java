package com.uppdragsradarn.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"userAssignments", "assignmentLocations", "assignmentSkills"})
public class Assignment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "company_name")
  private String companyName;

  @Column(name = "remote_percentage")
  private Integer remotePercentage;

  @Column(name = "duration_months")
  private Integer durationMonths;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "hourly_rate_min")
  private BigDecimal hourlyRateMin;

  @Column(name = "hourly_rate_max")
  private BigDecimal hourlyRateMax;

  @ManyToOne
  @JoinColumn(name = "currency_code")
  private Currency currency;

  @Column(name = "hours_per_week")
  private Integer hoursPerWeek;

  @Column(name = "application_deadline")
  private LocalDate applicationDeadline;

  @Column(name = "application_url")
  private String applicationUrl;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "source_id", nullable = false)
  private Source source;

  @Column(name = "external_id")
  private String externalId;

  @ManyToOne
  @JoinColumn(name = "status_id")
  private StatusType status;

  @Column(name = "active")
  private boolean active;

  @Column(name = "needs_manual_review")
  private boolean needsManualReview;

  @Column(name = "pii_detected", columnDefinition = "TEXT")
  private String piiDetected;

  // Transitional field for migration - not mapped to the database
  @Transient private String originalLocationText;

  public void setOriginalLocationText(String text) {
    this.originalLocationText = text;
  }

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<UserAssignment> userAssignments = new HashSet<>();

  @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<AssignmentLocation> assignmentLocations = new HashSet<>();

  @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<AssignmentSkill> assignmentSkills = new HashSet<>();

  /**
   * Get the formatted location string from normalized locations if available.
   *
   * @return The formatted location string for display
   */
  @Transient
  public String getFormattedLocation() {
    // If we have assignment locations, use the primary one or the first one
    if (this.assignmentLocations != null && !this.assignmentLocations.isEmpty()) {
      // Try to find the primary location first
      for (AssignmentLocation al : this.assignmentLocations) {
        if (al.isPrimary() && al.getLocation() != null) {
          return al.getLocation().getFormattedName();
        }
      }

      // If no primary, just use the first one
      AssignmentLocation first = this.assignmentLocations.iterator().next();
      if (first.getLocation() != null) {
        return first.getLocation().getFormattedName();
      }
    }

    return "Unknown location";
  }

  /**
   * Get the original location text for the assignment. This is a transitional method to handle
   * migration from direct location field to AssignmentLocation entities.
   *
   * @return The original location text, or null if not available
   */
  @Transient
  public String getOriginalLocationText() {
    // First check if we have the originalLocationText transient field set
    if (this.originalLocationText != null && !this.originalLocationText.isEmpty()) {
      return this.originalLocationText;
    }

    // Then check if we have assignment locations with original text
    if (this.assignmentLocations != null && !this.assignmentLocations.isEmpty()) {
      for (AssignmentLocation al : this.assignmentLocations) {
        if (al.isPrimary() && al.getOriginalText() != null && !al.getOriginalText().isEmpty()) {
          return al.getOriginalText();
        }
      }

      // If no primary with original text, check all locations
      for (AssignmentLocation al : this.assignmentLocations) {
        if (al.getOriginalText() != null && !al.getOriginalText().isEmpty()) {
          return al.getOriginalText();
        }
      }
    }

    // If we can't find anything, return null
    return null;
  }

  /**
   * Helper method to add a skill to the assignment
   *
   * @param skill The skill to add
   */
  public void addSkill(Skill skill) {
    AssignmentSkill assignmentSkill = new AssignmentSkill();
    assignmentSkill.setAssignment(this);
    assignmentSkill.setSkill(skill);
    this.assignmentSkills.add(assignmentSkill);
  }

  /**
   * Helper method to remove a skill from the assignment
   *
   * @param skill The skill to remove
   */
  public void removeSkill(Skill skill) {
    this.assignmentSkills.removeIf(as -> as.getSkill().equals(skill));
  }

  /**
   * Get a set of skills for this assignment
   *
   * @return A set of skills
   */
  @Transient
  public Set<Skill> getSkills() {
    Set<Skill> skills = new HashSet<>();
    for (AssignmentSkill as : this.assignmentSkills) {
      skills.add(as.getSkill());
    }
    return skills;
  }
}
