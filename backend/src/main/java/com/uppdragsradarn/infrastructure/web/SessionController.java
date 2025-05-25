package com.uppdragsradarn.infrastructure.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uppdragsradarn.application.service.UserService;
import com.uppdragsradarn.domain.model.User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

  private final UserService userService;

  @GetMapping
  public ResponseEntity<Map<String, Object>> getSessionInfo(
      @AuthenticationPrincipal OAuth2User principal, HttpServletRequest request) {

    Map<String, Object> response = new HashMap<>();

    // Debug logging
    log.debug("Session ID: " + request.getSession().getId());
    log.debug("Session cookies: " + request.getHeader("Cookie"));
    log.debug("Principal: " + principal);

    // Add CSRF token to the response
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (csrfToken != null) {
      response.put("csrfToken", csrfToken.getToken());
      log.debug("Added CSRF token to response: {}", csrfToken.getToken());
    } else {
      log.warn("CSRF token not found in request attributes");
    }

    if (principal == null) {
      response.put("authenticated", false);
      return ResponseEntity.ok(response);
    }

    response.put("authenticated", true);

    Map<String, Object> user = new HashMap<>();
    String email = principal.getAttribute("email");
    user.put("email", email);

    if (email != null) {
      userService
          .getUserByEmailOptional(email)
          .map(this::mapUserToResponse)
          .ifPresent(user::putAll);
    }

    response.put("user", user);
    return ResponseEntity.ok(response);
  }

  private Map<String, Object> mapUserToResponse(User user) {
    Map<String, Object> userData = new HashMap<>();
    userData.put("id", user.getId().toString());
    userData.put("notificationEmailEnabled", user.isNotificationEmailEnabled());
    userData.put("subscriptionTier", user.getSubscriptionTier());
    userData.put("name", user.getFirstName() + " " + user.getLastName());
    userData.put("given_name", user.getFirstName());
    userData.put("family_name", user.getLastName());
    // Add roles to the user data
    userData.put(
        "roles",
        user.getRoles().stream()
            .map(role -> role.getName())
            .collect(java.util.stream.Collectors.toSet()));
    return userData;
  }
}
