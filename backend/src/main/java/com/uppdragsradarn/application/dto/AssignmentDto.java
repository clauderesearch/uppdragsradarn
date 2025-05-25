package com.uppdragsradarn.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.uppdragsradarn.domain.model.Assignment;
import com.uppdragsradarn.domain.model.AssignmentLocation;
import com.uppdragsradarn.domain.model.Skill;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDto {

  private UUID id;

  @NotBlank private String title;

  private String description;

  private String companyName;

  private String location;

  private Integer remotePercentage;

  private Integer durationMonths;

  private LocalDate startDate;

  private BigDecimal hourlyRateMin;

  private BigDecimal hourlyRateMax;

  private String currency;

  private Integer hoursPerWeek;

  private Set<String> skills;

  private LocalDate applicationDeadline;

  private String applicationUrl;

  private SourceDto source;

  private boolean active;

  /**
   * Indicates if this assignment is only available to premium users. Free users will see this flag
   * but won't be able to view the actual assignment details.
   */
  private boolean premiumOnly;

  /**
   * Flag to indicate this is a limited version of the assignment for free users. When true, only
   * basic information is included (id, title, companyName, etc).
   */
  private boolean limitedVersion;

  /** Flag to indicate the assignment needs manual review due to PII detection */
  private boolean needsManualReview;

  /** Details about PII detected in the assignment */
  private String piiDetected;

  /** List of normalized locations associated with this assignment */
  private List<AssignmentLocationDto> normalizedLocations;

  public static AssignmentDto fromEntity(Assignment assignment) {
    return fromEntity(assignment, false);
  }

  public static AssignmentDto fromEntity(Assignment assignment, boolean limitedVersion) {
    boolean isPremiumOnly = isNewlyPosted(assignment);

    // Start with bare minimum for limited version
    AssignmentDto.AssignmentDtoBuilder builder =
        AssignmentDto.builder()
            .id(assignment.getId())
            .title(assignment.getTitle())
            .location(
                assignment
                    .getOriginalLocationText()) // Use transitional field for backward compatibility
            .source(
                assignment.getSource() != null
                    ? SourceDto.fromEntity(assignment.getSource())
                    : null)
            .active(assignment.isActive())
            .premiumOnly(isPremiumOnly)
            .limitedVersion(limitedVersion)
            .needsManualReview(assignment.isNeedsManualReview())
            .piiDetected(assignment.getPiiDetected());

    // Include normalized locations if available (important for all users)
    if (assignment.getAssignmentLocations() != null
        && !assignment.getAssignmentLocations().isEmpty()) {
      List<AssignmentLocationDto> locations = new ArrayList<>();
      for (AssignmentLocation al : assignment.getAssignmentLocations()) {
        locations.add(AssignmentLocationDto.fromEntity(al));
      }
      builder.normalizedLocations(locations);
    }

    // Only include detailed information for non-limited version
    if (!limitedVersion) {
      builder
          .companyName(assignment.getCompanyName())
          .description(assignment.getDescription())
          .remotePercentage(assignment.getRemotePercentage())
          .durationMonths(assignment.getDurationMonths())
          .startDate(assignment.getStartDate())
          .hourlyRateMin(assignment.getHourlyRateMin())
          .hourlyRateMax(assignment.getHourlyRateMax())
          .currency(assignment.getCurrency() != null ? assignment.getCurrency().getCode() : null)
          .hoursPerWeek(assignment.getHoursPerWeek())
          .skills(
              assignment.getSkills().stream()
                  .map(Skill::getName)
                  .collect(java.util.stream.Collectors.toSet()))
          .applicationDeadline(assignment.getApplicationDeadline())
          .applicationUrl(assignment.getApplicationUrl());
    }

    return builder.build();
  }

  /**
   * Determines if an assignment is newly posted (less than 48 hours old)
   *
   * @param assignment The assignment to check
   * @return true if the assignment is newly posted, false otherwise
   */
  private static boolean isNewlyPosted(Assignment assignment) {
    if (assignment.getCreatedAt() == null) {
      return false;
    }

    // Calculate hours since assignment was created
    long hoursSinceCreation =
        java.time.Duration.between(assignment.getCreatedAt(), java.time.LocalDateTime.now())
            .toHours();

    // Assignment is newly posted if it's less than 48 hours old
    return hoursSinceCreation < 48;
  }
}
