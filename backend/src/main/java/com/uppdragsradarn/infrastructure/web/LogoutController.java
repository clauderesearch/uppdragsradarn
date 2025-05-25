package com.uppdragsradarn.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/logout")
public class LogoutController {

  @Value("${app.frontend.url}")
  private String frontendUrl;

  @PostMapping
  public RedirectView logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    if (authentication != null) {
      new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    return new RedirectView(frontendUrl);
  }
}
