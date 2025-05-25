package com.uppdragsradarn.application.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uppdragsradarn.application.dto.UserDto;
import com.uppdragsradarn.application.dto.UserProfileUpdateDto;
import com.uppdragsradarn.application.service.UserService;
import com.uppdragsradarn.domain.exception.ResourceNotFoundException;
import com.uppdragsradarn.domain.model.Role;
import com.uppdragsradarn.domain.model.User;
import com.uppdragsradarn.domain.repository.RoleRepository;
import com.uppdragsradarn.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  private static final String SUBSCRIPTION_TIER_FREE = "FREE";

  @Override
  @Transactional
  public UserDto createUser(UserDto userDto) {
    if (userRepository.existsByEmail(userDto.getEmail())) {
      throw new IllegalArgumentException(
          "User with email " + userDto.getEmail() + " already exists");
    }

    User user = userDto.toEntity();
    user.setSubscriptionTier(SUBSCRIPTION_TIER_FREE);

    // Assign default USER role
    Role userRole =
        roleRepository
            .findByName("USER")
            .orElseThrow(() -> new IllegalStateException("Default USER role not found"));
    user.getRoles().add(userRole);

    user = userRepository.save(user);

    return UserDto.fromEntity(user);
  }

  @Override
  @Transactional
  public UserDto updateUser(UUID userId, UserDto userDto) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    user.setFirstName(userDto.getFirstName());
    user.setLastName(userDto.getLastName());
    user.setNotificationEmailEnabled(userDto.isNotificationEmailEnabled());

    user = userRepository.save(user);

    return UserDto.fromEntity(user);
  }

  @Override
  @Transactional
  public UserDto updateUserProfile(UUID userId, UserProfileUpdateDto profileUpdateDto) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    user.setFirstName(profileUpdateDto.getFirstName());
    user.setLastName(profileUpdateDto.getLastName());
    user.setNotificationEmailEnabled(profileUpdateDto.isNotificationEmailEnabled());

    user = userRepository.save(user);

    return UserDto.fromEntity(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserDto getUserById(UUID userId) {
    return userRepository
        .findById(userId)
        .map(UserDto::fromEntity)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
  }

  @Override
  @Transactional(readOnly = true)
  public UserDto getUserByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .map(UserDto::fromEntity)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserEntityById(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserEntityByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> getUserByEmailOptional(String email) {
    return userRepository.findByEmail(email);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && !authentication.getPrincipal().equals("anonymousUser")) {
      OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
      String email = oauth2User.getAttribute("email");

      if (email != null && !email.isEmpty()) {
        return userRepository.findByEmail(email);
      }
    }

    return Optional.empty();
  }

  @Override
  @Transactional
  public void deleteUser(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    userRepository.delete(user);
  }

  @Override
  @Transactional
  public void addRoleToUser(UUID userId, String roleName) {
    User user = getUserEntityById(userId);
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(
                () -> new ResourceNotFoundException("Role not found with name: " + roleName));

    user.getRoles().add(role);
    userRepository.save(user);
  }

  @Override
  @Transactional
  public void removeRoleFromUser(UUID userId, String roleName) {
    User user = getUserEntityById(userId);
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(
                () -> new ResourceNotFoundException("Role not found with name: " + roleName));

    user.getRoles().remove(role);
    userRepository.save(user);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean userHasRole(UUID userId, String roleName) {
    User user = getUserEntityById(userId);
    return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName));
  }
}
