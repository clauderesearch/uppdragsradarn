package com.uppdragsradarn.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDto {

  private String firstName;

  private String lastName;

  private boolean notificationEmailEnabled;
}
