package com.uppdragsradarn.application.service;

import java.util.Optional;
import java.util.UUID;

import com.uppdragsradarn.application.dto.UserDto;
import com.uppdragsradarn.application.dto.UserProfileUpdateDto;
import com.uppdragsradarn.domain.model.User;

public interface UserService {

  UserDto createUser(UserDto userDto);

  UserDto updateUser(UUID userId, UserDto userDto);

  UserDto updateUserProfile(UUID userId, UserProfileUpdateDto profileUpdateDto);

  UserDto getUserById(UUID userId);

  UserDto getUserByEmail(String email);

  User getUserEntityById(UUID userId);

  User getUserEntityByEmail(String email);

  Optional<User> getUserByEmailOptional(String email);

  Optional<User> getCurrentUser();

  void deleteUser(UUID userId);

  void addRoleToUser(UUID userId, String roleName);

  void removeRoleFromUser(UUID userId, String roleName);

  boolean userHasRole(UUID userId, String roleName);
}
