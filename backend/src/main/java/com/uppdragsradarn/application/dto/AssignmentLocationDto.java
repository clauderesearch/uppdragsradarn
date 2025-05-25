package com.uppdragsradarn.application.dto;

import java.util.UUID;

import com.uppdragsradarn.domain.model.AssignmentLocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Data Transfer Object for AssignmentLocation */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentLocationDto {

  private UUID id;
  private UUID assignmentId;
  private LocationDto location;
  private String originalText;
  private boolean remote;
  private Integer remotePercentage;
  private boolean primary;

  /**
   * Convert entity to DTO
   *
   * @param assignmentLocation AssignmentLocation entity
   * @return AssignmentLocationDto
   */
  public static AssignmentLocationDto fromEntity(AssignmentLocation assignmentLocation) {
    if (assignmentLocation == null) {
      return null;
    }

    return AssignmentLocationDto.builder()
        .id(assignmentLocation.getId())
        .assignmentId(assignmentLocation.getAssignment().getId())
        .location(LocationDto.fromEntity(assignmentLocation.getLocation()))
        .originalText(assignmentLocation.getOriginalText())
        .remote(assignmentLocation.isRemote())
        .remotePercentage(assignmentLocation.getRemotePercentage())
        .primary(assignmentLocation.isPrimary())
        .build();
  }
}
