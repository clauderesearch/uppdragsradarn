package com.uppdragsradarn.application.dto;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.uppdragsradarn.domain.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithRolesDto {

  private UUID id;
  private String email;
  private String firstName;
  private String lastName;
  private boolean notificationEmailEnabled;
  private String subscriptionTier;
  private Set<String> roles;

  public static UserWithRolesDto fromEntity(User user) {
    return UserWithRolesDto.builder()
        .id(user.getId())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .notificationEmailEnabled(user.isNotificationEmailEnabled())
        .subscriptionTier(user.getSubscriptionTier())
        .roles(user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()))
        .build();
  }

  public boolean isPremium() {
    return "PREMIUM".equals(this.subscriptionTier);
  }
}
