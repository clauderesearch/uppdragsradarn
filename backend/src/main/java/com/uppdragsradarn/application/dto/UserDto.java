package com.uppdragsradarn.application.dto;

import java.util.UUID;

import com.uppdragsradarn.domain.model.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

  private UUID id;

  @NotBlank @Email private String email;

  private String firstName;

  private String lastName;

  private boolean notificationEmailEnabled;

  private String subscriptionTier;

  public static UserDto fromEntity(User user) {
    return UserDto.builder()
        .id(user.getId())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .notificationEmailEnabled(user.isNotificationEmailEnabled())
        .subscriptionTier(user.getSubscriptionTier())
        .build();
  }

  public User toEntity() {
    return User.builder()
        .id(id)
        .email(email)
        .firstName(firstName)
        .lastName(lastName)
        .notificationEmailEnabled(notificationEmailEnabled)
        .subscriptionTier(subscriptionTier)
        .build();
  }

  public boolean isPremium() {
    return "PREMIUM".equals(this.subscriptionTier);
  }
}
