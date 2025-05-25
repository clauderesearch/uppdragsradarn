package com.uppdragsradarn.infrastructure.web.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(1)
@Slf4j
public class SessionCookieFilter extends OncePerRequestFilter {

  @Value("${app.cookie-domain:#{null}}")
  private String cookieDomain;

  @Value("${server.servlet.session.cookie.secure:false}")
  private boolean secureCookie;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(response);

    filterChain.doFilter(request, wrappedResponse);

    // After the request is processed, check if session cookie needs domain adjustment
    String sessionCookieHeader = response.getHeader("Set-Cookie");
    if (sessionCookieHeader != null && sessionCookieHeader.contains("JSESSIONID")) {
      log.debug("Original Set-Cookie header: {}", sessionCookieHeader);

      // Modify the cookie to include the domain if not already present
      if (cookieDomain != null && !sessionCookieHeader.contains("Domain=")) {
        String modifiedCookie = sessionCookieHeader;

        // Add domain attribute
        if (!modifiedCookie.contains("; Domain=")) {
          modifiedCookie += "; Domain=" + cookieDomain;
        }

        // Ensure SameSite=Lax is set
        if (!modifiedCookie.contains("; SameSite=")) {
          modifiedCookie += "; SameSite=Lax";
        }

        log.debug("Modified Set-Cookie header: {}", modifiedCookie);
        response.setHeader("Set-Cookie", modifiedCookie);
      }
    }
  }

  private static class HttpServletResponseWrapper
      extends jakarta.servlet.http.HttpServletResponseWrapper {
    public HttpServletResponseWrapper(HttpServletResponse response) {
      super(response);
    }
  }
}
