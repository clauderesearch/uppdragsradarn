package com.uppdragsradarn.application.dto;

import java.util.UUID;

import com.uppdragsradarn.domain.model.StatusType;
import com.uppdragsradarn.domain.model.UserAssignment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAssignmentDto {

  private UUID id;

  private UUID userId;

  @NotNull private UUID assignmentId;

  @NotNull private UUID statusId;

  private String statusName;

  private String notes;

  public static UserAssignmentDto fromEntity(UserAssignment userAssignment) {
    return UserAssignmentDto.builder()
        .id(userAssignment.getId())
        .userId(userAssignment.getUser().getId())
        .assignmentId(userAssignment.getAssignment().getId())
        .statusId(userAssignment.getStatus().getId())
        .statusName(userAssignment.getStatus().getName())
        .notes(userAssignment.getNotes())
        .build();
  }

  public UserAssignment toEntity(StatusType status) {
    return UserAssignment.builder().id(id).status(status).notes(notes).build();
  }
}
